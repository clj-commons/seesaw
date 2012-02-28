;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.graphics
  (:use seesaw.graphics
        [seesaw.color :only [to-color]])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [java.awt RenderingHints]
           [java.awt.image BufferedImage]))

(describe anti-alias
  (it "turns on anti-aliasing on a graphics object"
    (let [bi (buffered-image 100 100)
          g2d (.getGraphics bi)]
      (anti-alias g2d)
      (expect (= RenderingHints/VALUE_ANTIALIAS_ON (.getRenderingHint g2d RenderingHints/KEY_ANTIALIASING))))))

(describe to-paint
  (it "returns its input if it's a java.awt.Paint"
    (= java.awt.Color/BLACK (to-paint java.awt.Color/BLACK)))
  (it "falls back to to-color otherwise"
    (= (to-color :black) (to-paint :black))))

(describe line
  (it "creates a line shape with given end points"
    (let [l (line 1 2 3 4)]
      (expect (= java.awt.geom.Line2D$Double (class l)))
      (expect (= [1.0 2.0 3.0 4.0] [(.x1 l) (.y1 l) (.x2 l) (.y2 l)])))))

(describe rect
  (it "creates a rectangle shape with give corner, width and height"
    (let [r (rect 1 2 3 4)]
      (expect (= java.awt.geom.Rectangle2D$Double (class r)))
      (expect (= [1.0 2.0 3.0 4.0] [(.x r) (.y r) (.width r) (.height r)]))))
  (it "creates a rectangle shape with give corner, adjusting for negative width and height"
    (let [r (rect 10 20 -3 -4)]
      (expect (= java.awt.geom.Rectangle2D$Double (class r)))
      (expect (= [7.0 16.0 3.0 4.0] [(.x r) (.y r) (.width r) (.height r)]))))
  (it "creates a square with give corner, and side length"
    (let [r (rect 1 2 3)]
      (expect (= java.awt.geom.Rectangle2D$Double (class r)))
      (expect (= [1.0 2.0 3.0 3.0] [(.x r) (.y r) (.width r) (.height r)])))))

(describe rounded-rect
  (it "creates a rounded rectangle shape with give corner, width and height and radii"
    (let [r (rounded-rect 1 2 3 4 5 6)]
      (expect (= java.awt.geom.RoundRectangle2D$Double (class r)))
      (expect (= [1.0 2.0 3.0 4.0] [(.x r) (.y r) (.width r) (.height r)]))
      (expect (= [5.0 6.0] [(.arcwidth r) (.archeight r)]))))
  (it "creates a rounded rectangle shape with give corner, negative width and height and radii"
    (let [r (rounded-rect 10 20 -3 -4 5 6)]
      (expect (= java.awt.geom.RoundRectangle2D$Double (class r)))
      (expect (= [7.0 16.0 3.0 4.0] [(.x r) (.y r) (.width r) (.height r)]))
      (expect (= [5.0 6.0] [(.arcwidth r) (.archeight r)]))))
  (it "creates a rounded rectangle shape with give corner, width and height and radius"
    (let [r (rounded-rect 1 2 3 4 5)]
      (expect (= java.awt.geom.RoundRectangle2D$Double (class r)))
      (expect (= [1.0 2.0 3.0 4.0] [(.x r) (.y r) (.width r) (.height r)]))
      (expect (= [5.0 5.0] [(.arcwidth r) (.archeight r)])))))

(describe ellipse
  (it "creates an elliptical shape with give corner, width and height"
    (let [r (ellipse 1 2 3 4)]
      (expect (= java.awt.geom.Ellipse2D$Double (class r)))
      (expect (= [1.0 2.0 3.0 4.0] [(.x r) (.y r) (.width r) (.height r)]))))
  (it "creates an elliptical shape with give corner, negative width and height"
    (let [r (ellipse 11 12 -3 -4)]
      (expect (= java.awt.geom.Ellipse2D$Double (class r)))
      (expect (= [8.0 8.0 3.0 4.0] [(.x r) (.y r) (.width r) (.height r)]))))
  (it "creates a square with give corner, and side length"
    (let [r (ellipse 1 2 3)]
      (expect (= java.awt.geom.Ellipse2D$Double (class r)))
      (expect (= [1.0 2.0 3.0 3.0] [(.x r) (.y r) (.width r) (.height r)])))))

(describe circle
  (it "creates a circle with center and radius"
    (let [r (circle 4 5 6)]
      (expect (= java.awt.geom.Ellipse2D$Double (class r)))
      (expect (= [-2.0 -1.0 12.0 12.0] [(.x r) (.y r) (.width r) (.height r)])))))

(describe arc
  (it "creates an arc shape with corner, width, height and angle"
    (let [s (arc 1 2 3 4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/OPEN (.getArcType s)))
      (expect (= [1.0 2.0 3.0 4.0 0.0 360.0]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)]))))
  (it "creates an arc shape with corner, negative width, negative height and angle"
    (let [s (arc 12 22 -3 -4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/OPEN (.getArcType s)))
      (expect (= [9.0 18.0 3.0 4.0 0.0 360.0]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)])))))

(describe chord
  (it "creates an chord shape with corner, width, height and angle"
    (let [s (chord 1 2 3 4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/CHORD (.getArcType s)))
      (expect (= [1.0 2.0 3.0 4.0 0.0 360.0]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)]))))
  (it "creates an chord shape with corner, negative width, negative height and angle"
    (let [s (chord 10 21 -3 -4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/CHORD (.getArcType s)))
      (expect (= [7.0 17.0 3.0 4.0 0.0 360.0]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)])))))

(describe pie
  (it "creates an pie shape with corner, width, height and angle"
    (let [s (pie 1 2 3 4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/PIE (.getArcType s)))
      (expect (= [1.0 2.0 3.0 4.0 0.0 360.0]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)]))))
  (it "creates an pie shape with corner, negative width, negative height and angle"
    (let [s (pie 11 20 -3 -4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/PIE (.getArcType s)))
      (expect (= [8.0 16.0 3.0 4.0 0.0 360.0]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)])))))

(describe string-shape
  (it "creates a string shape"
    (string-shape 1 2 "HI")))

(describe stroke
  (it "creates a default stroke of width 1 with no args"
    (let [s (stroke)]
      (expect (= java.awt.BasicStroke (class s)))
      (expect (= 1.0 (.getLineWidth s)))))
  (it "creates a stroke with the given properties"
    (let [s (stroke :width 10, :cap :butt, :join :bevel, :miter-limit 15.0,
                    :dashes [10.0 5.0],
                    :dash-phase 2.0)]
      (expect (= java.awt.BasicStroke (class s)))
      (expect (= 10. (.getLineWidth s)))
      (expect (= java.awt.BasicStroke/CAP_BUTT (.getEndCap s)))
      (expect (= 15.0 (.getMiterLimit s)))
      (expect (= [10.0 5.0] (seq (.getDashArray s))))
      (expect (= 2.0 (.getDashPhase s)))
      (expect (= java.awt.BasicStroke/JOIN_BEVEL (.getLineJoin s))))))

(describe to-stroke
  (it "throws IllegalArgumentException if it doesn't know what to do"
    (try
      (do (to-stroke #"what?") false)
      (catch IllegalArgumentException e true)))
  (it "returns nil for nil input"
    (nil? (to-stroke nil)))
  (it "returns a stroke of a given width if input is a number"
    (= 10.0 (.getLineWidth (to-stroke 10))))
  (it "returns input if it's a stroke"
    (let [s (stroke)]
      (expect (= s (to-stroke s))))))

; make a stub shape to grab args...
(defrecord TestShape [received-args]
  Draw
  (draw* 
    [shape g2d style] 
    ; Note that "this" is used instead of shape. Otherwise, on failure, lazytest
    ; tries to print a cyclical structure when there's a failure.
    (swap! received-args conj [g2d "this" style])))

(describe draw
  (it "should call Draw/draw* with graphics, shape and style"
    (let [args (atom [])
          ts (TestShape. args)
          result (draw "graphics" ts "style")
          final-args @args]
      (expect (= "graphics" result))
      (expect (= [["graphics" "this" "style"]] final-args))))

  (it "should call Draw/draw* with graphics, and multiple shapes and styles"
    (let [args (atom [])
          ts (TestShape. args)
          result (draw "graphics" ts "style" ts "style2")
          final-args @args]
      (expect (= "graphics" result))
      (expect (= [["graphics" "this" "style"]["graphics" "this" "style2"]] final-args)))))
  
(describe style
  (it "creates a new style object"
    (let [strk (stroke :width 5)
          s (style :foreground :black :background :white :stroke strk :font :monospace)]
      (expect (= java.awt.Color/BLACK (:foreground s)))
      (expect (= java.awt.Color/WHITE (:background s)))
      (expect (= strk (:stroke s)))
      (expect (not (nil? (:font s)))))))

(describe update-style
  (it "constructs a new style with new property values"
    (let [strk (stroke :width 5)
          s (update-style (style :foreground :black :stroke strk) :foreground :white :background :black)]
      (expect (instance? seesaw.graphics.Style s))
      (expect (= java.awt.Color/WHITE (:foreground s)))
      (expect (= java.awt.Color/BLACK (:background s)))
      (expect (= strk (:stroke s)))))
  (it "constructs a new style and can clear property values"
    (let [s (update-style (style :foreground :black) :foreground nil)]
      (expect (instance? seesaw.graphics.Style s))
      (expect (nil? (:foreground s))))))

(describe linear-gradient
  (it "creates a default linear gradient"
    (let [g (linear-gradient)]
      (expect (= (java.awt.geom.Point2D$Float. 0.0 0.0)
                 (.getStartPoint g)))
      (expect (= (java.awt.geom.Point2D$Float. 1.0 0.0)
                 (.getEndPoint g)))
      (expect (= [(float 0.0) (float 1.0)]
                 (vec (.getFractions g))))
      (expect (= [java.awt.Color/WHITE java.awt.Color/BLACK]
                 (vec (.getColors g))))
      (expect (= java.awt.MultipleGradientPaint$CycleMethod/NO_CYCLE
                 (.getCycleMethod g)))))
  (it "creates a linear gradient"
    (let [g (linear-gradient 
              :start [1 2] 
              :end [3.5 4.6]
              :fractions [0.0 0.8 1.0]
              :colors [:black :blue java.awt.Color/ORANGE]
              :cycle :repeat)]
      (expect (= (java.awt.geom.Point2D$Float. 1.0 2.0)
                 (.getStartPoint g)))
      (expect (= (java.awt.geom.Point2D$Float. 3.5 4.6)
                 (.getEndPoint g)))
      (expect (= [(float 0.0) (float 0.8) (float 1.0)]
                 (vec (.getFractions g))))
      (expect (= [java.awt.Color/BLACK java.awt.Color/BLUE java.awt.Color/ORANGE]
                 (vec (.getColors g))))
      (expect (= java.awt.MultipleGradientPaint$CycleMethod/REPEAT
                 (.getCycleMethod g))))))

(describe radial-gradient
  (it "creates a default radial gradient"
    (let [g (radial-gradient)]
      (expect (= (java.awt.geom.Point2D$Float. 0.0 0.0)
                 (.getCenterPoint g)))
      (expect (= (java.awt.geom.Point2D$Float. 0.0 0.0)
                 (.getFocusPoint g)))
      (expect (= (float 1.0) (.getRadius g)))
      (expect (= [(float 0.0) (float 1.0)]
                 (vec (.getFractions g))))
      (expect (= [java.awt.Color/WHITE java.awt.Color/BLACK]
                 (vec (.getColors g))))
      (expect (= java.awt.MultipleGradientPaint$CycleMethod/NO_CYCLE
                 (.getCycleMethod g)))))
  (it "creates a radial gradient"
    (let [g (radial-gradient 
              :center [1 2] 
              :focus [3.5 4.6]
              :fractions [0.0 0.8 1.0]
              :colors [:black :blue java.awt.Color/ORANGE]
              :cycle :reflect)]
      (expect (= (java.awt.geom.Point2D$Float. 1.0 2.0)
                 (.getCenterPoint g)))
      (expect (= (java.awt.geom.Point2D$Float. 3.5 4.6)
                 (.getFocusPoint g)))
      (expect (= [(float 0.0) (float 0.8) (float 1.0)]
                 (vec (.getFractions g))))
      (expect (= [java.awt.Color/BLACK java.awt.Color/BLUE java.awt.Color/ORANGE]
                 (vec (.getColors g))))
      (expect (= java.awt.MultipleGradientPaint$CycleMethod/REFLECT
                 (.getCycleMethod g))))))

