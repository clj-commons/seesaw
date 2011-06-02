;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.util
  (:require clojure.string)
  (:use [seesaw meta])
  (:import [java.net URL MalformedURLException]))

(defmacro preferences-node
  "Return the java.util.prefs.Preferences/userRoot for the current
  namespace."
  []
  `(.node (java.util.prefs.Preferences/userRoot) ~(str (ns-name *ns*))))

(defn gen-pref-atom
  "Generate and return an atom, which will automatically be synced
  with (java.util.prefs.Preferences/userRoot) for the current
  namespace and a given string KEY. If not yet set, the atom will have
  INITIAL-VALUE as its value, or the value which has already been set
  inside the preferences. Note that the value must be printable per
  PRINT-DUP and readable per READ-STRING for it to be used with the
  preferences store."  [key initial-value]
  (let [v (atom (read-string (.get (preferences-node) key (binding [*print-dup* true] (pr-str initial-value)))))]
    (add-watch v (keyword (gensym "pref-atom-watcher"))
               (fn [k r o n] (when (not= o n) 
                               (.put (preferences-node) key (binding [*print-dup* true] (pr-str n))))))))

(defn check-args 
  [condition message]
  (if-not condition
    (throw (IllegalArgumentException. message))
    true))
  
(defmacro cond-doto
  "Spawn of (cond) and (doto). Works like (doto), but each form has a condition
   which controls whether it is executed. Returns x.

  (doto (new java.util.HashMap) 
    true    (.put \"a\" 1) 
    (< 2 1) (.put \"b\" 2))
  
  Here, only (.put \"a\" 1) is executed.
  "
  [x & forms]
    (let [gx (gensym)]
      `(let [~gx ~x]
         ~@(map (fn [[c a]]
                  (if (seq? a)
                    `(when ~c (~(first a) ~gx ~@(next a)))
                    `(when ~c (~a ~gx))))
                (partition 2 forms))
         ~gx)))

(defn to-seq [v]
  "Stupid helper to turn possibly single values into seqs"
  (cond 
    (nil? v) v
    (seq? v)  v
    (coll? v) (seq v)
    :else (seq [v])))

(defn- constantize-keyword [k]
  (.. (name k) (toUpperCase) (replace "-" "_")))

(defn constant-map
  "Given a class and a list of keywordized constant names returns the 
   values of those fields in a map. The name mapping upper-cases and replaces
   hyphens with underscore, e.g.
 
    :above-baseline --> ABOVE_BASELINE

   Note that the fields must be static and declared *in* the class, not a 
   supertype.
  "
  [klass & fields]
  (reduce
    (fn [m [k v]] (assoc m k v))
    {}
    (map 
      #(vector %1 (.. klass (getDeclaredField (constantize-keyword %1)) (get nil)))
      fields)))
    
  
(defn camelize
  "Convert input string to camelCase from hyphen-case"
  [s]
  (clojure.string/replace s #"-(.)" #(.toUpperCase (%1 1))))

(defn boolean? [b]
  "Return true if b is exactly true or false. Useful for handling optional
   boolean properties where we want to do nothing if the property isn't 
   provided."
  (or (true? b) (false? b)))

(defn atom? [a]
  "Return true if a is an atom"
  (isa? (type a) clojure.lang.Atom))

(defn try-cast [c x]
  "Just like clojure.core/cast, but returns nil on failure rather than throwing ClassCastException"
  (try
    (cast c x)
    (catch ClassCastException e nil)))

(defn to-url [s]
  "Try to parse (str s) as a URL. Returns new java.net.URL on success, nil 
  otherwise. This is different from clojure.java.io/as-url in that it doesn't
  throw an exception and it uses (str) on the input."
  (try
    (URL. (str s))
    (catch MalformedURLException e nil)))

(def ^{:private true} options-property "seesaw-creation-options")

(defn- store-option-handlers
  [target handler-map]
  (put-meta! target options-property handler-map))

(defn- get-option-handlers
  [target]
  (get-meta target options-property))

(defn apply-options
  [target opts handler-map]
  (check-args (or (map? opts) (even? (count opts))) 
              "opts must be a map or have an even number of entries")
  (doseq [[k v] (if (map? opts) opts (partition 2 opts))]
    (if-let [f (get handler-map k)]
      (f target v)
      (throw (IllegalArgumentException. (str "Unknown option " k)))))
  (store-option-handlers target handler-map))

(defn reapply-options
  [target args default-options]
  (let [options (or (get-option-handlers target) default-options)]
    (apply-options target args options)))

(defn to-dimension
  [v]
  (cond
    (instance? java.awt.Dimension v) v
    (and (vector? v) (= 3 (count v)) (= :by (second v)))
      (let [[w by h] v] (java.awt.Dimension. w h))
    :else (throw (IllegalArgumentException. "v must be a Dimension or [w :by h]"))))

(defn children [c]
  (seq 
    ; TODO PROTOCOL!
    (cond
      (instance? javax.swing.JFrame c) (if-let [mb (.getJMenuBar c)] 
                                         (cons mb (.getComponents c)) 
                                         (.getComponents c))
      (instance? javax.swing.JMenuBar c) (.getSubElements c)
      (instance? javax.swing.JMenu c)    (.getSubElements c)
      (isa? (type c) java.awt.Container)    (.getComponents c)
      :else nil)))

(defn collect
  "Given a root widget or frame, returns a depth-fist seq of all the widgets
  in the hierarchy. For example to disable everything:
  
    (config (collect (.getContentPane my-frame)) :enabled? false)
  "
  [root]
  (tree-seq 
    (constantly true) 
    children
    root))

