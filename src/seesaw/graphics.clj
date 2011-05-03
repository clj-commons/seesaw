;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.graphics
  (:use [seesaw util color font])
  (:import [java.awt Graphics2D RenderingHints]
           [java.awt.image BufferedImage]))

(defn anti-alias 
  "Enable anti-aliasing on the given Graphics2D object"
  [g2d]
  (.setRenderingHint g2d RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON))

(defn buffered-image 
  ([width height] (buffered-image width height BufferedImage/TYPE_INT_ARGB))
  ([width height t] (BufferedImage. width height t)))

(defmacro push 
  "Push a Graphics2D context (Graphics2d/create) and automatically dispose it.
  
  For example, in a paint handler:
  
    (fn [c g2d]
      (.setColor g2d java.awt.Color/RED)
      (.drawString g2d \"This string is RED\" 0 20)
      (push g2d
        (.setColor g2d java.awt.Color/BLUE)
        (.drawString g2d \"This string is BLUE\" 0 40))
      (.drawString g2d \"This string is RED again\" 0 60))
  "
  [g2d & forms]
  `(let [~g2d (. ~g2d create)]
     (try 
       ~@forms
       (finally
         (. ~g2d dispose)))))

; coords
; 2 points
; point + dimension
(defn line [x1 y1 x2 y2] (java.awt.geom.Line2D$Double. x1 y1 x2 y2))

; coords
; 2 points
; point + dimension
(defn rect [x y w h]     (java.awt.geom.Rectangle2D$Double. x y w h))
(defn rounded-rect [x y w h rx ry] (java.awt.geom.RoundRectangle2D$Double. x y w h rx ry))

(defn ellipse [x y w h]  (java.awt.geom.Ellipse2D$Double. x y w h))

(defn arc 
  ([x y w h start extent arc-type]
    (java.awt.geom.Arc2D$Double x y w h start extent arc-type))
  ([x y w h start extent]
    (arc x y w h start extent java.awt.geom.Arc2D/OPEN)))

(defn chord 
  [x y w h start extent]
  (arc x y w h start extent java.awt.geom.Arc2D/CHORD))

(defn pie 
  [x y w h start extent]
  (arc x y w h start extent java.awt.geom.Arc2D/PIE))

(defn polygon 
  [points]
  (let [p (java.awt.Polygon.)]
    (doseq [[x y] points]
      (.addPoint p x y))
    p))

(defn stroke
  [& {:keys [width]}]
  (java.awt.BasicStroke. (or width 1)))

(defn rotate    [g2d theta] (.rotate g2d theta) g2d)
(defn translate [g2d dx dy] (.translate g2d dx dy) g2d)

(defn style 
  [& opts]
  (apply hash-map opts))

(defrecord StringShape [x y value])
(defn string [x y value] (StringShape. x y value))

(defprotocol Draw
  (draw* [shape g2d])
  (fill* [shape g2d]))

(extend-type java.awt.Shape Draw
  (draw* [shape g2d] (.draw g2d shape))
  (fill* [shape g2d] (.fill g2d shape)))

(extend-type StringShape Draw
  (draw* [shape g2d] (.drawString g2d (:value shape) (:x shape) (:y shape)))
  (fill* [shape g2d] ))

(defn draw [g2d shape style]
  (let [fg (:foreground style)
        fg (and fg (to-color fg))
        bg (:background style)
        bg (and bg (to-color bg))
        s  (:stroke style)]
    (when bg
      (do 
        (.setColor g2d bg) 
        (fill* shape g2d)))
    (when fg
      (do 
        (when s   (do (.setStroke g2d s)))
        (.setColor g2d fg) 
        (draw* shape g2d)))
    g2d))

