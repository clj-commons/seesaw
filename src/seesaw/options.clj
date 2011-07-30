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
        (. ~target ~(setter-name bean-property-name) (~(or set-conv identity) value#)))
      (fn [~(with-meta target {:tag target-type})]
        (~(or get-conv identity) (. ~target ~(getter-name bean-property-name)))))))

(defn default-option 
  ([name] (default-option name (fn [_ _] (throw (IllegalArgumentException. (str "No setter defined for option " name))))))
  ([name setter] (default-option name setter (fn [_] (throw (IllegalArgumentException. (str "No getter defined for option " name))))))
  ([name setter getter] (Option. name setter getter)))

(defn ignore-option
  "Might be used to explicitly ignore the default behaviour of options."
  [name]
  (default-option name (fn [_ _]) (fn [_ _])))

(defn apply-options
  [target opts handler-map]
  (check-args (or (map? opts) (even? (count opts))) 
              "opts must be a map or have an even number of entries")
  (doseq [[k v] (if (map? opts) opts (partition 2 opts))]
    (if-let [^Option opt (get handler-map k)]
      (if-let [setter (.setter opt)] 
        (setter target v)
        (throw (IllegalArgumentException. (str "No setter found for option " k))))
      (throw (IllegalArgumentException. (str "Unknown option " k)))))
  (store-option-handlers target handler-map))

(defn reapply-options
  [target args default-options]
  (let [options (or (get-option-value-handlers target) default-options)]
    (apply-options target args options)))

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

