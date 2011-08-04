;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.graphics
  (:use seesaw.graphics)
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

(describe line
  (it "creates a line shape with given end points"
    (let [l (line 1 2 3 4)]
      (expect (= java.awt.geom.Line2D$Double (class l)))
      (expect (= [1 2 3 4] [(.x1 l) (.y1 l) (.x2 l) (.y2 l)])))))

(describe rect
  (it "creates a rectangle shape with give corner, width and height"
    (let [r (rect 1 2 3 4)]
      (expect (= java.awt.geom.Rectangle2D$Double (class r)))
      (expect (= [1 2 3 4] [(.x r) (.y r) (.width r) (.height r)]))))
  (it "creates a rectangle shape with give corner, adjusting for negative width and height"
    (let [r (rect 10 20 -3 -4)]
      (expect (= java.awt.geom.Rectangle2D$Double (class r)))
      (expect (= [7 16 3 4] [(.x r) (.y r) (.width r) (.height r)]))))
  (it "creates a square with give corner, and side length"
    (let [r (rect 1 2 3)]
      (expect (= java.awt.geom.Rectangle2D$Double (class r)))
      (expect (= [1 2 3 3] [(.x r) (.y r) (.width r) (.height r)])))))

(describe rounded-rect
  (it "creates a rounded rectangle shape with give corner, width and height and radii"
    (let [r (rounded-rect 1 2 3 4 5 6)]
      (expect (= java.awt.geom.RoundRectangle2D$Double (class r)))
      (expect (= [1 2 3 4] [(.x r) (.y r) (.width r) (.height r)]))
      (expect (= [5 6] [(.arcwidth r) (.archeight r)]))))
  (it "creates a rounded rectangle shape with give corner, negative width and height and radii"
    (let [r (rounded-rect 10 20 -3 -4 5 6)]
      (expect (= java.awt.geom.RoundRectangle2D$Double (class r)))
      (expect (= [7 16 3 4] [(.x r) (.y r) (.width r) (.height r)]))
      (expect (= [5 6] [(.arcwidth r) (.archeight r)]))))
  (it "creates a rounded rectangle shape with give corner, width and height and radius"
    (let [r (rounded-rect 1 2 3 4 5)]
      (expect (= java.awt.geom.RoundRectangle2D$Double (class r)))
      (expect (= [1 2 3 4] [(.x r) (.y r) (.width r) (.height r)]))
      (expect (= [5 5] [(.arcwidth r) (.archeight r)])))))

(describe ellipse
  (it "creates an elliptical shape with give corner, width and height"
    (let [r (ellipse 1 2 3 4)]
      (expect (= java.awt.geom.Ellipse2D$Double (class r)))
      (expect (= [1 2 3 4] [(.x r) (.y r) (.width r) (.height r)]))))
  (it "creates an elliptical shape with give corner, negative width and height"
    (let [r (ellipse 11 12 -3 -4)]
      (expect (= java.awt.geom.Ellipse2D$Double (class r)))
      (expect (= [8 8 3 4] [(.x r) (.y r) (.width r) (.height r)]))))
  (it "creates a square with give corner, and side length"
    (let [r (ellipse 1 2 3)]
      (expect (= java.awt.geom.Ellipse2D$Double (class r)))
      (expect (= [1 2 3 3] [(.x r) (.y r) (.width r) (.height r)])))))

(describe circle
  (it "creates a circle with center and radius"
    (let [r (circle 4 5 6)]
      (expect (= java.awt.geom.Ellipse2D$Double (class r)))
      (expect (= [-2 -1 12 12] [(.x r) (.y r) (.width r) (.height r)])))))

(describe arc
  (it "creates an arc shape with corner, width, height and angle"
    (let [s (arc 1 2 3 4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/OPEN (.getArcType s)))
      (expect (= [1 2 3 4 0 360]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)]))))
  (it "creates an arc shape with corner, negative width, negative height and angle"
    (let [s (arc 12 22 -3 -4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/OPEN (.getArcType s)))
      (expect (= [9 18 3 4 0 360]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)])))))

(describe chord
  (it "creates an chord shape with corner, width, height and angle"
    (let [s (chord 1 2 3 4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/CHORD (.getArcType s)))
      (expect (= [1 2 3 4 0 360]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)]))))
  (it "creates an chord shape with corner, negative width, negative height and angle"
    (let [s (chord 10 21 -3 -4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/CHORD (.getArcType s)))
      (expect (= [7 17 3 4 0 360]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)])))))

(describe pie
  (it "creates an pie shape with corner, width, height and angle"
    (let [s (pie 1 2 3 4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/PIE (.getArcType s)))
      (expect (= [1 2 3 4 0 360]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)]))))
  (it "creates an pie shape with corner, negative width, negative height and angle"
    (let [s (pie 11 20 -3 -4 0 360)]
      (expect (= java.awt.geom.Arc2D$Double (class s)))
      (expect (= java.awt.geom.Arc2D/PIE (.getArcType s)))
      (expect (= [8 16 3 4 0 360]
                 [(.x s) (.y s) (.width s) (.height s) (.start s) (.extent s)])))))

(describe stroke
  (it "creates a default stroke of width 1 with no args"
    (let [s (stroke)]
      (expect (= java.awt.BasicStroke (class s)))
      (expect (= 1 (.getLineWidth s)))))
  (it "creates a stroke with the given properties"
    (let [s (stroke :width 10, :cap :butt, :join :bevel, :miter-limit 15.0,
                    :dashes [10.0 5.0],
                    :dash-phase 2.0)]
      (expect (= java.awt.BasicStroke (class s)))
      (expect (= 10 (.getLineWidth s)))
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
    (= 10 (.getLineWidth (to-stroke 10))))
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
  

