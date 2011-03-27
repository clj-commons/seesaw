(ns seesaw.color
  (:import [java.awt Color]))

(defn get-rgba 
  [c] 
  (vector (.getRed c) (.getGreen c) (.getBlue c) (.getAlpha c)))

(defn color
  ([s] (Color/decode s))
  ([s a] (apply color (assoc (get-rgba (color s)) 3 a)))
  ([r g b a] (Color. r (or g 0) (or b 0) (or a 255)))
  ([r g b] (color r g b nil)))

(defn to-color
  [c]
  (cond
    (instance? Color c) c
    true (color c))) 
    
