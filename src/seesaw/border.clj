;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for creating widget borders."
      :author "Dave Ray"}
  seesaw.border
  (:use [seesaw.color :only [to-color]]
        [seesaw.util :only [to-insets]])
  (:import [javax.swing BorderFactory]
           [javax.swing.border Border]
           [java.awt Color]))

;*******************************************************************************
; Borders

(declare to-border)

(defn empty-border 
  [& {:keys [thickness top left bottom right]}]
  (if (or top left bottom right)
    (BorderFactory/createEmptyBorder (or top 0) (or left 0) (or bottom 0) (or right 0))
    (let [t (or thickness 1)]
      (BorderFactory/createEmptyBorder t t t t))))

(defn line-border 
  [& {:keys [color thickness top left bottom right] :or {thickness 1 color Color/BLACK}}]
  (if (or top left bottom right)
    (BorderFactory/createMatteBorder 
      (int (or top 0)) (int (or left 0)) (int (or bottom 0)) (int (or right 0)) 
      ^Color (to-color color))
    (BorderFactory/createLineBorder (to-color color) thickness)))

(defn compound-border
  ([b] (to-border b))
  ([b0 b1] (BorderFactory/createCompoundBorder (to-border b1) (to-border b0)))
  ([b0 b1 & more] (reduce #(compound-border %1 %2) (compound-border b0 b1) more)))

(defn custom-border 
  "Define a custom border with the following properties:
  
    :paint A function that takes the same arguments as Border.paintBorder:
             java.awt.Component c - The target component
              java.awt.Graphics g - The graphics context to draw to
                            int x - x position of border
                            int y - y position of border
                            int w - width of border
                            int h - height of border
 
    :insets Returns the insets of the border. Can be a zero-arg function that
              returns something that is passed through (seesaw.util/to-insets)
              or a constant value passed through the same. Defaults to 0.

    :opaque? Whether the border is opaque. A constant truthy value or a zero-arg
             function that returns a truthy value.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/border/Border.html
    (seesaw.util/to-insets)
  "
  [& args]
  (let [{:keys [insets opaque? paint]} args
        insets (cond 
                 (fn? insets) insets
                 :else (constantly insets))
        opaque? (cond
                  (fn? opaque?) opaque?
                  :else (constantly opaque?))] 
    (reify javax.swing.border.Border
      (getBorderInsets [this c]
        (to-insets (insets c)))
      (isBorderOpaque [this]
        (boolean (opaque?)))
      (paintBorder [this c g x y w h]
        (when paint (paint c g x y w h))))))

(defn to-border 
  ([b] 
    (cond
      (instance? Border b) b
      (integer? b)         (empty-border :thickness b)
      (coll? b)            (apply to-border b)
      true                 (BorderFactory/createTitledBorder (str b))))
  ([b & args]
    (apply compound-border b args)))

