;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for dealing with options."
      :author "Dave Ray"}
  seesaw.options
  (:use [seesaw util meta]))

(defrecord Option [name setter getter])

(def ^{:private true} options-property "seesaw-options")

(defn- store-option-handlers
  [target handler-map]
  (put-meta! target options-property handler-map))

(defn- get-option-value-handlers
  [target]
  (get-meta target options-property))

(defn- strip-question-mark
  [^String s] 
  (if (.endsWith ^String s "?")
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
  [name-arg target-type & [set-conv get-conv]]
  (let [[option-name bean-property-name] (split-bean-option-name name-arg)
        target (gensym "target")]
  `(Option. ~option-name 
      (fn [~(with-meta target {:tag target-type}) value#]
        (. ~target ~(setter-name bean-property-name) (~(or set-conv 'identity) value#)))
      (fn [~(with-meta target {:tag target-type})]
        (~(or get-conv 'identity) (. ~target ~(getter-name bean-property-name)))))))

(defn default-option 
  ([name] (default-option name (fn [_ _] (throw (IllegalArgumentException. (str "No setter defined for option " name))))))
  ([name setter] (default-option name setter (fn [_] (throw (IllegalArgumentException. (str "No getter defined for option " name))))))
  ([name setter getter] (Option. name setter getter)))

(defn ignore-option
  "Might be used to explicitly ignore the default behaviour of options."
  [name]
  (default-option name (fn [_ _]) (fn [_ _])))

(declare reapply-options)

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
      {:pre [(keyword? value) (namespace value)]}
      (let [nspace (namespace value)
            prefix (name value)]
            (reapply-options
              target (mapcat (fn [k]
                              (let [prop (keyword nspace (str prefix "." k))]
                                (when-let [v (resource prop)]
                                  [(keyword k) v])))
                            (map name keys))
              nil)))))

(defn- apply-option
  [target ^Option opt v]
  (if-let [setter (.setter opt)] 
    (setter target v)
    (throw (IllegalArgumentException. (str "No setter found for option " (.name opt))))))

(defn- ^Option lookup-option [handler-map name]
  (if-let [opt (get handler-map name)]
    opt
    (throw (IllegalArgumentException. (str "Unknown option " name)))))

(defn- apply-options*
  [target opts handler-map]
  (let [pairs (if (map? opts) opts (partition 2 opts))] 
    (doseq [[k v] pairs]
      (let [opt (lookup-option handler-map k)]
        (apply-option target opt v))))
  target)

(defn apply-options
  [target opts handler-map]
  (check-args (or (map? opts) (even? (count opts))) 
              "opts must be a map or have an even number of entries")
  (store-option-handlers target handler-map)
  (apply-options* target opts handler-map))

(defn reapply-options
  [target args default-options]
  (let [options (or (get-option-value-handlers target) default-options)]
    (apply-options* target args options)))

(defn ignore-options
  "Create a ignore-map for options, which should be ignored. Ready to
  be merged into default option maps."
  [source-options]
  (into {} (for [k (keys source-options)] [k (ignore-option k)])))

(defn get-option-value 
  ([target name] (get-option-value target name (get-option-value-handlers target)))
  ([target name handlers]
    (let [^Option option (get handlers name)
          getter (:getter option)]
      (if getter
        (getter target)
        (throw (IllegalArgumentException. (str "No getter found for option " name)))))))

