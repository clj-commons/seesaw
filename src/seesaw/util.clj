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

(defmacro let-kw
  "Adds flexible keyword handling to any form which has a parameter
   list: fn, defn, defmethod, letfn, and others. Keywords may be
   passed to the surrounding form as & rest arguments, lists, or
   maps. Lists or maps must be used for functions with multiple
   arities if more than one arity has keyword parameters. Keywords are
   bound inside let-kw as symbols, with default values either
   specified in the keyword spec or nil. Keyword specs may consist of
   just the bare keyword symbol, which defaults to nil, or may have
   the general form [keyword-name keyword-default-value*
   keyword-supplied?*].  keyword-supplied?  is an optional symbol
   bound to true if the keyword was supplied, and to false otherwise."
  [kw-spec-raw kw-args & body]
  (let [kw-spec  (map #(if (sequential? %) % [%]) kw-spec-raw)
        symbols  (map first kw-spec)
        keywords (map (comp keyword name) symbols)
        defaults (map second kw-spec)
        destrmap {:keys (vec symbols) :or (zipmap symbols defaults)}
        supplied (reduce
                  (fn [m [k v]] (assoc m k v)) (sorted-map)
                  (remove (fn [[_ val]] (nil? val))
                          (partition 2 (interleave
                                        keywords
                                        (map (comp second rest)
                                             kw-spec)))))
        kw-args-map (gensym)]
    `(let [kw-args# ~kw-args
           ~kw-args-map (if (map? kw-args#)
                          kw-args#
                          (apply hash-map kw-args#))
           ~destrmap ~kw-args-map]
       ~@(if (empty? supplied)
           body
           `((apply (fn [~@(vals supplied)]
                      ~@body)
                    (map (fn [x#] (contains? ~kw-args-map x#))
                         [~@(keys supplied)])))))))

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
      :else    (.getComponents c))))

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

