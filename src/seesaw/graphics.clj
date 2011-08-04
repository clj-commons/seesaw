;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Basic graphics functions to simplify use of Graphics2D."
      :author "Dave Ray"}
  seesaw.graphics
  (:use [seesaw util color font])
  (:import [java.awt Graphics2D RenderingHints]
           [java.awt.image BufferedImage]))

(defn anti-alias 
  "Enable anti-aliasing on the given Graphics2D object.
  
  Returns g2d."
  [^java.awt.Graphics2D g2d]
  (doto g2d 
    (.setRenderingHint RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)))

(defn buffered-image 
  ([width height]   (buffered-image width height BufferedImage/TYPE_INT_ARGB))
  ([width height t] (BufferedImage. width height t)))

(defn- to-image [v]
  (cond
    (nil? v) nil
    (instance? java.awt.Image v) v
    (instance? javax.swing.ImageIcon v) (.getImage ^javax.swing.ImageIcon v)
    :else (throw (IllegalArgumentException. (str "Don't know how to make image from " v)))))

;*******************************************************************************
; Graphics context state

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

;*******************************************************************************
; Basic shapes

(defn line [x1 y1 x2 y2] (java.awt.geom.Line2D$Double. x1 y1 x2 y2))

(defn rect 
  "
  Create a rectangular shape with the given upper-left corner, width and 
  height.
  "
  ([x y w h] (java.awt.geom.Rectangle2D$Double.
                (if (> w 0) x (+ x w)) 
                (if (> h 0) y (+ y h))
                (Math/abs w)
                (Math/abs h)
                                        ))
  ([x y w] (rect x y w w)))


(defn rounded-rect 
  "
  Create a rectangular shape with the given upper-left corner, width,
  height and corner radii.
  "
  ([x y w h rx ry] (java.awt.geom.RoundRectangle2D$Double.
                      (if (> w 0) x (+ x w)) 
                      (if (> h 0) y (+ y h))
                      (Math/abs w)
                      (Math/abs h)
                      rx ry))
  ([x y w h rx] (rounded-rect x y w h rx rx))
  ([x y w h] (rounded-rect x y w h 5)))

(defn ellipse 
  "Create an ellipse that occupies the given rectangular region"
  ([x y w h]  (java.awt.geom.Ellipse2D$Double.
                      (if (> w 0) x (+ x w)) 
                      (if (> h 0) y (+ y h))
                      (Math/abs w)
                      (Math/abs h)))
  ([x y w]  (ellipse x y w w)))

(defn circle 
  "Create a circle with the given center and radius"
  [x y radius] 
  (ellipse (- x radius) (- y radius) (* radius 2)))

(defn arc 
  ([x y w h start extent arc-type]
    (java.awt.geom.Arc2D$Double.
      (if (> w 0) x (+ x w)) 
      (if (> h 0) y (+ y h))
      (Math/abs w)
      (Math/abs h)
      start extent arc-type))
  ([x y w h start extent]
    (arc x y w h start extent java.awt.geom.Arc2D/OPEN)))

(defn chord 
  [x y w h start extent]
  (arc x y w h start extent java.awt.geom.Arc2D/CHORD))

(defn pie 
  [x y w h start extent]
  (arc x y w h start extent java.awt.geom.Arc2D/PIE))

(defn polygon 
  "Create a polygonal shape with the given set of vertices.
  points is a list of x/y pairs, e.g.:

    (polygon [1 2] [3 4] [5 6])
  "
  [& points]
  (let [p (java.awt.Polygon.)]
    (doseq [[x y] points]
      (.addPoint p x y))
    p))

(def ^{:private true} path-ops {
  'line-to '.lineTo
  'move-to '.moveTo
  'curve-to '.curveTo
  'quad-to 'quad-to
})

(defmacro path [opts & forms]
  (when (not (vector? opts)) (throw (IllegalArgumentException. "path must start with vector of (possibly empty) options")))
  (let [p (gensym "path")]
    `(let [~p (java.awt.geom.Path2D$Double.)]
       ; Insert an initial moveTo to avoid needless exceptions
       (.moveTo ~p 0 0)
       ~@(for [f forms]
           (cons (get path-ops (first f)) (cons p (rest f))))
       ~p)))

(defrecord StringShape [x y value])
(defn string-shape [x y value] (StringShape. x y value))

(defrecord ImageShape [x y image])

(defn image-shape [x y image]
  (ImageShape. x y (to-image image)))

;*******************************************************************************
; Coordinate transforms

(defn rotate
  "Apply a rotation to the graphics context by degrees
  
  Returns g2d"
  [^java.awt.Graphics2D g2d degrees] 
  (.rotate g2d (Math/toRadians degrees)) g2d)

(defn translate 
  "Apply a translation to the graphics context
  
  Returns g2d"
  [^java.awt.Graphics2D g2d dx dy] (.translate g2d (double dx) (double dy)) g2d)

(defn scale
  "Apply a scale factor to the graphics context

  Returns g2d"
  ([^java.awt.Graphics2D g2d sx sy] (.scale g2d sx sy) g2d)
  ([^java.awt.Graphics2D g2d s]     (.scale g2d s s) g2d))

;*******************************************************************************
; Strokes

(def ^{:private true} stroke-caps {
  :square java.awt.BasicStroke/CAP_SQUARE
  :butt   java.awt.BasicStroke/CAP_BUTT
  :round  java.awt.BasicStroke/CAP_ROUND
})

(def ^{:private true} stroke-joins {
  :bevel java.awt.BasicStroke/JOIN_BEVEL
  :miter java.awt.BasicStroke/JOIN_MITER
  :round java.awt.BasicStroke/JOIN_ROUND
})

(defn stroke
  "Create a new stroke with the given properties:

    :width Width of the stroke
  "
  [& {:keys [width cap join miter-limit dashes dash-phase] 
      :or {width 1 cap :square join :miter miter-limit 10.0 dashes nil dash-phase 0.0}}]
  (java.awt.BasicStroke. width 
                         (stroke-caps cap) 
                         (stroke-joins join) 
                         miter-limit 
                         (when (seq dashes) (float-array dashes))
                         dash-phase))

(defn to-stroke [v]
  "Convert v to a stroke. As follows depending on v:
  
    nil - returns nil
    java.awt.Stroke instance - returns v

   Throws IllegalArgumentException if it can't figure out what to do.
   "
  (cond
    (nil? v)    nil
    (number? v) (stroke :width v)
    (instance? java.awt.Stroke v) v
    :else (throw (IllegalArgumentException. (str "Don't know how to make a stroke from " v)))))

(def ^{:private true} default-stroke (stroke))

;*******************************************************************************
; Styles

(defrecord Style [foreground background stroke font])

(defn style 
  [& {:keys [foreground background stroke font]}]
  (Style. 
    (to-color foreground) 
    (to-color background)
    (to-stroke stroke)
    (to-font font)))

;*******************************************************************************
; Shape drawing protocol

(defprotocol Draw
  (draw* [shape ^java.awt.Graphics2D g2d style]))

(extend-type java.awt.Shape Draw
  (draw* [shape ^java.awt.Graphics2D g2d style] 
    (let [fg (to-color (:foreground style))
          bg (to-color (:background style))
          s  (or (:stroke style) default-stroke)]
      (when bg
        (do 
          (.setColor g2d bg) 
          (.fill g2d shape)))
      (when fg
        (do 
          (.setStroke g2d s)
          (.setColor g2d fg) 
          (.draw g2d shape))))))

(extend-type StringShape Draw
  (draw* [shape ^java.awt.Graphics2D g2d style]
    (let [fg (to-color (:foreground style))
          f  (:font style)]
      (when f (.setFont g2d f))
      (.setColor g2d (or fg java.awt.Color/BLACK))
      (.drawString g2d ^String (:value shape) (float (:x shape)) (float (:y shape))))))

(extend-type ImageShape Draw
  (draw* [shape ^java.awt.Graphics2D g2d style] 
    (.drawImage g2d ^java.awt.Image (:image shape) ^Integer (:x shape) ^Integer (:y shape) nil)))

(defn draw 
  "Draw a one or more shape/style pairs to the given graphics context.

  shape should be an object that implements Draw protocol (see (rect), 
  (ellipse), etc. 
  
  style is a style object created with (style). If the style's :foreground
  is non-nil, the border of the shape is drawn with the given stroke. If
  the style's :background is non-nil, the shape is filled with that color.

  Returns g2d.
  "
  ([g2d] g2d)
  ([g2d shape style]
      (draw* shape g2d style)
      g2d)
  ([g2d shape style & more]
   (let [ret (draw g2d shape style)]
     (if more
       (recur ret (first more) (second more) (nnext more))
       ret))))

