;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for creating Swing colors. Note that these are implicit
            in the core color options."
      :author "Dave Ray"}
  seesaw.color
  (:import [java.awt Color]))

(defn get-rgba 
  [c] 
  (vector (.getRed c) (.getGreen c) (.getBlue c) (.getAlpha c)))

(defn color
  ([s] (Color/decode s))
  ([s a] (apply color (assoc (get-rgba (color s)) 3 a)))
  ([r g b a] (Color. r (or g 0) (or b 0) (or a 255)))
  ([r g b] (color r g b nil)))

(defn default-color
  "Retrieve a default color from the UIManager.

  Examples:

    ; Return the look and feel's label foreground color
    (default-color \"Label.foreground\")

  Returns a java.awt.Color instance or nil if not found.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/UIManager.html#getColor%28java.lang.Object%29
  "
  [name]
  (.getColor (javax.swing.UIManager/getDefaults) name))

(defn to-color
  [c]
  (cond
    (nil? c)            nil
    (instance? Color c) c
    :else               (color c))) 
    
