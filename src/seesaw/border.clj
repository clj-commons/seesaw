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
        [seesaw.util  :only [to-insets resource resource-key?]])
  (:import [javax.swing BorderFactory]
           [javax.swing.border Border]
           [java.awt Color]))

;*******************************************************************************
; Borders

(declare to-border)

(defn empty-border 
  "Create an empty border. The following properties are supported:
  
    :thickness The thickness of the border (all sides) in pixels. This property
               is only used if :top, :bottom, etc are omitted. Defaults to 1.
  
    :top       Thickness of the top border in pixels. Defaults to 0.
    :left      Thickness of the left border in pixels. Defaults to 0.
    :bottom    Thickness of the bottom border in pixels. Defaults to 0.
    :right     Thickness of the right border in pixels. Defaults to 0.

  Examples:

      ; Create an empty 10 pixel border
      (empty-border :thickness 10)

      ; Create an empty border 5 pixels on top and left, 0 on other sides
      (empty-border :left 5 :top 5)
  "
  [& {:keys [thickness top left bottom right]}]
  (if (or top left bottom right)
    (BorderFactory/createEmptyBorder (or top 0) (or left 0) (or bottom 0) (or right 0))
    (let [t (or thickness 1)]
      (BorderFactory/createEmptyBorder t t t t))))

(defn line-border 
  "Create a colored border with following properties:
  
    :color The color, passed through (seesaw.color/to-color). Defaults to black.
    :thickness The thickness of the border in pixels. This property is only used
               if :top, :bottom, etc are omitted. Defaults to 1.
    :top       Thickness of the top border in pixels. Defaults to 0.
    :left      Thickness of the left border in pixels. Defaults to 0.
    :bottom    Thickness of the bottom border in pixels. Defaults to 0.
    :right     Thickness of the right border in pixels. Defaults to 0.
 
  Examples: 
    
      ; Create a green border, 3 pixels on top, 5 pixels on the botttom
      (line-border :color \"#0f0\" :top 3 :bottom 5)
  "
  [& {:keys [color thickness top left bottom right] :or {thickness 1 color Color/BLACK}}]
  (if (or top left bottom right)
    (BorderFactory/createMatteBorder 
      (int (or top 0)) (int (or left 0)) (int (or bottom 0)) (int (or right 0)) 
      ^Color (to-color color))
    (BorderFactory/createLineBorder (to-color color) thickness)))

(defn compound-border
  "Create a compount border from the given arguments. Order is from inner to outer.
  Each argument is passed through (seesaw.border/to-border).
  
  Examples:
    
      ; Create an 4 pixel empty border, red line border, and title border.
      (compound-border 4 (line-border :color :red :thickness 4) \"Title\")

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/BorderFactory.html
  "
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
  "Construct a border. The border returned depends on the input:

    nil - returns nil
    a Border - returns b
    a number - returns an empty border with the given thickness
    a vector or list - returns a compound border by applying to-border
                       to each element, inner to outer.
    a i18n keyword   - returns a titled border using the given resource
    a string         - returns a titled border using the given stirng

  If given more than one argument, a compound border is created by applying
  to-border to each argument, inner to outer.


  Note:

  to-border is used implicitly by the :border option supported by all widgets
  to it is rarely necessary to call directly.
  "
  ([b] 
    (cond
      (nil? b)             nil
      (instance? Border b) b
      (integer? b)         (empty-border :thickness b)
      (coll? b)            (apply to-border b)
      (resource-key? b)    (to-border (resource b))
      :else                (BorderFactory/createTitledBorder (str b))))
  ([b & args]
    (apply compound-border b args)))

