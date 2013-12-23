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
  (:use [seesaw.color :only [to-color]]
        [seesaw.font :only [to-font]]
        [seesaw.util :only [illegal-argument]])
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
    :else (illegal-argument "Don't know how to make image from %s" v)))

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
                (Math/abs (double w))
                (Math/abs (double h))
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
                      (Math/abs (double w))
                      (Math/abs (double h))
                      rx ry))
  ([x y w h rx] (rounded-rect x y w h rx rx))
  ([x y w h] (rounded-rect x y w h 5)))

(defn ellipse
  "Create an ellipse that occupies the given rectangular region"
  ([x y w h]  (java.awt.geom.Ellipse2D$Double.
                      (if (> w 0) x (+ x w))
                      (if (> h 0) y (+ y h))
                      (Math/abs (double w))
                      (Math/abs (double h))))
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
      (Math/abs (double w))
      (Math/abs (double h))
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
  (when (not (vector? opts)) (illegal-argument "path must start with vector of (possibly empty) options"))
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
; Gradients

(defn- ^java.awt.geom.Point2D$Float to-point2d-f [[x y]] (java.awt.geom.Point2D$Float. (float x) (float y)))
(def ^{:private true} default-start [0 0])
(def ^{:private true} default-end [1 0])
(def ^{:private true} default-fractions [0.0 1.0])
(def ^{:private true} default-colors [java.awt.Color/WHITE java.awt.Color/BLACK])
(def ^{:private true} cycle-map
  {:none    java.awt.MultipleGradientPaint$CycleMethod/NO_CYCLE
   :repeat  java.awt.MultipleGradientPaint$CycleMethod/REPEAT
   :reflect java.awt.MultipleGradientPaint$CycleMethod/REFLECT })

(defn linear-gradient
  "Creates a linear gradient suitable for use on the :foreground and
  :background properties of a (seesaw.graphics/style), or anywhere
  a java.awt.Paint is required. Has the following options:

    :start The start [x y] point, defaults to [0 0]
    :end   The end [x y] point, defaults to [1 0]
    :fractions Sequence of fractional values indicating color transition points
               in the gradient. Defaults to [0.0 1.0]. Must have same number
               of entries as :colors.
    :colors Sequence of color values correspoding to :fractions. Value is passed
            through (seesaw.color/to-color). e.g. :blue, \"#fff\", etc.
    :cycle The cycle behavior of the gradient, :none, :repeat, or :reflect.
           Defaults to :none.

  Examples:

    ; create a horizontal red, white and blue gradiant with three equal parts
    (linear-gradient :fractions [0 0.5 1.0] :colors [:red :white :blue])

  See:
    http://docs.oracle.com/javase/6/docs/api/java/awt/LinearGradientPaint.html
  "
  [& {:keys [start end fractions colors cycle]
      :or {start     default-start
           end       default-end
           fractions default-fractions
           colors    default-colors
           cycle     :none }
      :as opts}]
    (java.awt.LinearGradientPaint.
      (to-point2d-f start)
      (to-point2d-f end)
      (float-array fractions)
      (into-array java.awt.Color (map to-color colors))
      (cycle-map cycle)))

(def ^{:private true} default-center [0 0])
(def ^{:private true} default-radius 1.0)

(defn radial-gradient
  "Creates a radial gradient suitable for use on the :foreground and
  :background properties of a (seesaw.graphics/style), or anywhere
  a java.awt.Paint is required. Has the following options:

    :center The center [x y] point, defaults to [0 0]
    :focus The focus [x y] point, defaults to :center
    :radius   The radius. Defaults to 1.0
    :fractions Sequence of fractional values indicating color transition points
               in the gradient. Defaults to [0.0 1.0]. Must have same number
               of entries as :colors.
    :colors Sequence of color values correspoding to :fractions. Value is passed
            through (seesaw.color/to-color). e.g. :blue, \"#fff\", etc.
    :cycle The cycle behavior of the gradient, :none, :repeat, or :reflect.
           Defaults to :none.

  Examples:

    ; create a red, white and blue gradiant with three equal parts
    (radial-gradient :radius 100.0 :fractions [0 0.5 1.0] :colors [:red :white :blue])

  See:
    http://docs.oracle.com/javase/6/docs/api/java/awt/RadialGradientPaint.html
  "
  [& {:keys [center focus radius fractions colors cycle]
      :or {center    default-center
           radius    default-radius
           fractions default-fractions
           colors    default-colors
           cycle     :none }
      :as opts}]
    (java.awt.RadialGradientPaint.
      (to-point2d-f center)
      (float radius)
      (to-point2d-f (or focus center))
      (float-array fractions)
      ^{:tag "[Ljava.awt.Color;"} (into-array java.awt.Color (map to-color colors))
      ^java.awt.MultipleGradientPaint$CycleMethod (cycle-map cycle)))

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

(defn to-stroke
  "Convert v to a stroke. As follows depending on v:

    nil - returns nil
    java.awt.Stroke instance - returns v

   Throws IllegalArgumentException if it can't figure out what to do.
   "
  [v]
  (cond
    (nil? v)    nil
    (number? v) (stroke :width v)
    (instance? java.awt.Stroke v) v
    :else (illegal-argument "Don't know how to make a stroke from %s" v)))

(def ^{:private true} default-stroke (stroke))

;*******************************************************************************
; Styles

(defn ^java.awt.Paint to-paint
  [v]
  (cond
    (instance? java.awt.Paint v) v
    :else (to-color v)))

(defrecord Style [^java.awt.Paint  foreground
                  ^java.awt.Paint  background
                  ^java.awt.Stroke stroke
                  ^java.awt.Font   font])

(defn style
  "Create a new style object for use with (seesaw.graphics/draw). Takes a list
  of key/value pairs:

    :foreground A color value (see seesaw.color) for the foreground (stroke)
    :background A color value (see seesaw.color) for the background (fill)
    :stroke     A stroke value used to draw outlines (see seesaw.graphics/stroke)
    :font       Font value used for drawing text shapes

    The default value for all properties is nil. See (seesaw.graphics/draw) for
    interpretation of nil values.

  Notes:

    Style objects are immutable so they can be efficiently \"pre-compiled\" and
    used for drawing multiple shapes.

  Examples:

    ; Red on black
    (style :foreground :red :background :black :font :monospace)

    ; Red, 8-pixel line with no fill.
    (style :foreground :red :stroke 8)

  See:
    (seesaw.graphics/update-style)
    (seesaw.graphics/draw)
  "
  [& {:keys [foreground background stroke font]}]
  (Style.
    (to-paint foreground)
    (to-paint background)
    (to-stroke stroke)
    (to-font font)))

(defn update-style
  "Update a style with new properties and return a new style. This is basically
  exactly the same as (clojure.core/assoc) with the exception that color, stroke,
  and font values are interpreted by Seesaw.

  Examples:

    (def start (style :foreground blue :background :white))
    (def no-fill (update-style start :background nil))
    (def red-line (update-style no-fill :foreground :red))

  See:
    (seesaw.graphics/style)
    (seesaw.graphics/draw)
  "
  [s & {:keys [foreground background stroke font]
        :or { foreground (:foreground s) ; Preserve original value and watch out for nil
              background (:background s)
              stroke (:stroke s)
              font (:font s) }}]
  (Style.
    (to-paint foreground)
    (to-paint background)
    (to-stroke stroke)
    (to-font font)))

;*******************************************************************************
; Shape drawing protocol

(defprotocol Draw
  (draw* [shape ^java.awt.Graphics2D g2d style]))

(extend-type java.awt.Shape Draw
  (draw* [shape ^java.awt.Graphics2D g2d style]
    (let [fg (:foreground style)
          bg (:background style)
          s  (or (:stroke style) default-stroke)]
      (when bg
        (do
          (.setPaint g2d bg)
          (.fill g2d shape)))
      (when fg
        (do
          (.setStroke g2d s)
          (.setPaint g2d fg)
          (.draw g2d shape))))))

(extend-type StringShape Draw
  (draw* [shape ^java.awt.Graphics2D g2d style]
    (let [fg (:foreground style)
          f  (:font style)]
      (when f (.setFont g2d f))
      (.setPaint g2d (or fg java.awt.Color/BLACK))
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

