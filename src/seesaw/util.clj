(ns seesaw.util
  (:import [java.net URL MalformedURLException]))

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

