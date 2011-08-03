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
  (:import [java.net URL MalformedURLException]))

(defn check-args 
  [condition message]
  (if-not condition
    (throw (IllegalArgumentException. ^String message))
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
  [^Class klass & fields]
  (let [[options fields] (if (map? (first fields)) [(first fields) (rest fields)] [{} fields])
        {:keys [suffix] :or {suffix ""}} options]
    (reduce
      (fn [m [k v]] (assoc m k v))
      {}
      (map 
        #(vector %1 (.. klass 
                      (getDeclaredField (str (constantize-keyword %1) suffix)) 
                      (get nil)))
        fields))))
    
  
(defn camelize
  "Convert input string to camelCase from hyphen-case"
  [s]
  (clojure.string/replace s #"-(.)" #(.toUpperCase ^String (%1 1))))

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

(defn ^URL to-url [s]
  "Try to parse (str s) as a URL. Returns new java.net.URL on success, nil 
  otherwise. This is different from clojure.java.io/as-url in that it doesn't
  throw an exception and it uses (str) on the input."
  (if (instance? URL s) s
  (try
    (URL. (str s))
    (catch MalformedURLException e nil))))


(defn to-dimension
  [v]
  (cond
    (instance? java.awt.Dimension v) v
    (and (vector? v) (= 3 (count v)) (= :by (second v)))
      (let [[w by h] v] (java.awt.Dimension. w h))
    :else (throw (IllegalArgumentException. "v must be a Dimension or [w :by h]"))))

(defn to-insets
  [v]
  (cond
    (instance? java.awt.Insets v) v
    (number? v) (java.awt.Insets. v v v v)
    (vector? v) (let [[top left bottom right] v]
                  (java.awt.Insets. top left (or bottom top) (or right left)))
    :else (throw (IllegalArgumentException. (str "Don't know how to create insets from " v)))))

(defprotocol Children 
  "A protocol for retrieving the children of a widget as a seq. 
  This takes care of idiosyncracies of frame vs. menus, etc."

  (children [c] "Returns a seq of the children of the given widget"))

(extend-protocol Children
  ; Thankfully container covers JComponent, JFrame, dialogs, applets, etc.
  java.awt.Container    (children [this] (seq (.getComponents this)))
  ; Special case for menus. We want the logical menu items, not whatever
  ; junk is used to build them.
  javax.swing.JMenuBar  (children [this] (seq (.getSubElements this)))
  javax.swing.JMenu     (children [this] (seq (.getSubElements this))))

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

(defn reverse-map 
  "Reverse the direction of a map"
  [m]
  (into {} (for [[k v] m] [v k])))

