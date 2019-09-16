;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc    "Functions for dealing with options."
      :author "Dave Ray"}
seesaw.options
  (:use [seesaw.util :only [camelize illegal-argument check-args
                            resource resource-key?]])
  (:import (clojure.lang IAtom IDeref IMeta IRef)))

(defprotocol OptionProvider
  (get-option-maps* [this]))

(defn get-option-map [this]
  (apply merge (get-option-maps* this)))

(defmacro option-provider [class options]
  `(extend-protocol OptionProvider
     ~class
     (~'get-option-maps* [this#] [~options])))

(defrecord Option [name setter getter examples])

(declare apply-options)

(defn- strip-question-mark
  [^String s]
  (if (.endsWith s "?")
    (.substring s 0 (dec (count s)))
    s))

(defn- setter-name [property]
  (->> property
       name
       strip-question-mark
       (str "set-")
       camelize
       symbol))

(defn- getter-name [property]
  (let [property (name property)
        prefix   (if (.endsWith property "?") "is-" "get-")]
    (->> property
         name
         strip-question-mark
         (str prefix)
         camelize
         symbol)))

(defn- split-bean-option-name [v]
  (cond
    (vector? v) v
    :else [v v]))

(defmacro bean-option
  [name-arg target-type & [set-conv get-conv examples]]
  (let [[option-name bean-property-name] (split-bean-option-name name-arg)
        target (gensym "target")]
    `(Option. ~option-name
              (fn [~(with-meta target {:tag target-type}) value#]
                (. ~target ~(setter-name bean-property-name) (~(or set-conv `identity) value#)))
              (fn [~(with-meta target {:tag target-type})]
                (~(or get-conv `identity) (. ~target ~(getter-name bean-property-name))))
              ~examples)))

(defn default-option
  ([name] (default-option name (fn [_ _] (illegal-argument "No setter defined for option %s" name))))
  ([name setter] (default-option name setter (fn [_] (illegal-argument "No getter defined for option %s" name))))
  ([name setter getter] (default-option name setter getter nil))
  ([name setter getter examples] (Option. name setter getter examples)))

(defn ignore-option
  "Might be used to explicitly ignore the default behaviour of options."
  ([name examples] (default-option name (fn [_ _]) (fn [_ _]) "Internal use."))
  ([name] (ignore-option name nil)))

(defn resource-option
  "Defines an option that takes a j18n namespace-qualified keyword as a
  value. The keyword is used as a prefix for the set of properties in
  the given key list. This allows subsets of widget options to be configured
  from a resource bundle.
  
  Example:
    ; The :resource property looks in a resource bundle for 
    ; prefix.text, prefix.foreground, etc.
    (resource-option :resource [:text :foreground :background])
  "
  [option-name keys]
  (default-option
    option-name
    (fn [target value]
      {:pre [(resource-key? value)]}
      (let [nspace (namespace value)
            prefix (name value)]
        (apply-options
          target (mapcat (fn [k]
                           (let [prop (keyword nspace (str prefix "." k))]
                             (when-let [v (resource prop)]
                               [(keyword k) v])))
                         (map name keys)))))
    nil
    [(str "A i18n prefix for a resource with keys")
     (pr-str keys)]))

;;TODO there is a small memory leak, find out how to fix it!
(def satoms (atom {}))

(defn- apply-setter
  [this old-val new-val]
  (when-not (= old-val new-val)
    (doseq [[ins m] (get @satoms this)]
      (doseq [opt (vals m)]
        (if-let [setter (:setter opt)]
          (if (seq (:keys opt))
            (let [o-val (apply get-in (cons old-val [(:keys opt)]))
                  n-val (apply get-in (cons new-val [(:keys opt)]))]
              (when-not (= o-val n-val)
                (setter ins n-val)))
            (setter ins new-val))
          (illegal-argument "No setter found for option %s" (:name opt))))))
  new-val)

(deftype SAtom [state meta validator watches]

  IAtom
  (swap [this f]
    (apply-setter this @state (swap! state f)))
  (swap [this f x]
    (apply-setter this @state (swap! state f x)))
  (swap [this f x y]
    (apply-setter this @state (swap! state f x y)))
  (swap [this f x y args]
    (apply-setter this @state (swap! state f x y args)))
  (compareAndSet [this old new]
    (when (compare-and-set! state old new)
      (apply-setter this @state new)))
  (reset [this new]
    (apply-setter this @state (reset! state new)))

  IDeref
  (deref [_]
    @state)

  IRef
  (addWatch [_ k call]
    (.addWatch state k call))
  (removeWatch [_ k]
    (.removeWatch state k))

  IMeta
  (meta [_] meta))

(defn get-k
  [^SAtom a k & ks]
  {:satom a :keys (cons k ks)})

(defn setup-reference*
  [^clojure.lang.ARef r options]
  (let [opts (apply hash-map options)]
    (when (:meta opts)
      (.resetMeta r (:meta opts)))
    (when (:validator opts)
      (.setValidator r (:validator opts)))
    r))

(defn satom
  ([x] (SAtom. (atom x) nil nil nil))
  ([x & options] (setup-reference* (satom x) options)))

(defn- apply-option
  [target ^Option opt v]
  (if-let [setter (:setter opt)]
    (cond
      (and (map? v) (= (class (:satom v)) SAtom))
      (do
        (swap! satoms assoc-in [(:satom v) target (:name opt)] (assoc opt :keys (:keys v)))
        (setter target (apply get-in (cons @(:satom v) [(:keys v)]))))

      (= SAtom (class v))
      (do
        (swap! satoms assoc-in [v target (:name opt)] opt)
        (setter target @v))

      :else
      (setter target v))
    (illegal-argument "No setter found for option %s" (:name opt))))

(defn- ^Option lookup-option [target handler-maps name]
  ;(println "---------------------------")
  ;(println handler-maps)
  (if-let [opt (some #(if % (% name)) handler-maps)]
    opt
    (illegal-argument "%s does not support the %s option" (class target) name)))

(defn- apply-options*
  [target opts handler-maps]
  (let [pairs (if (map? opts) opts (partition 2 opts))]
    (doseq [[k v] pairs]
      (let [opt (lookup-option target handler-maps k)]
        (apply-option target opt v))))
  target)

(defn apply-options
  [target opts]
  (check-args (or (map? opts) (even? (count opts)))
              "opts must be a map or have an even number of entries")
  (apply-options* target opts (get-option-maps* target)))

(defn ignore-options
  "Create a ignore-map for options, which should be ignored. Ready to
  be merged into default option maps."
  [source-options]
  (into {} (for [k (keys source-options)] [k (ignore-option k)])))

(defn around-option
  ([parent-option set-conv get-conv examples]
   (default-option (:name parent-option)
                   (fn [target value]
                     ((:setter parent-option) target ((or set-conv identity) value)))
                   (fn [target]
                     ((or get-conv identity) ((:getter parent-option) target)))
                   examples))
  ([parent-option set-conv get-conv]
   (around-option parent-option set-conv get-conv nil)))

(defn option-map
  "Construct an option map from a list of options."
  [& opts]
  (into {} (map (juxt :name identity) opts)))

(defn get-option-value
  ([target name] (get-option-value target name (get-option-maps* target)))
  ([target name handlers]
   (let [^Option option (lookup-option target handlers name)
         getter         (:getter option)]
     (if getter
       (getter target)
       (illegal-argument "Option %s cannot be read from %s" name (class target))))))

(defn set-option-value
  ([target name value] (set-option-value target name (get-option-maps* target)))
  ([target name value handlers]
   (let [^Option option (lookup-option target handlers name)
         setter         (:setter option)]
     (if setter
       (setter target value)
       (illegal-argument "Option %s cannot be set on %s" name (class target))))))

