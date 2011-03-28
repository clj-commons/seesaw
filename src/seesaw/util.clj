(ns seesaw.util
  (:import [java.net URL MalformedURLException]))


(defn try-cast [c x]
  "Just like clojure.core/cast, but returns nil on failure rather than throwing ClassCastException"
  (try
    (cast c x)
    (catch ClassCastException e nil)))

(defn to-url [s]
  "Try to parse (str s) as a URL. Returns new java.net.URL on success, nil otherwise."
  (try
    (URL. (str s))
    (catch MalformedURLException e nil)))

