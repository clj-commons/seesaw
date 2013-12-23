;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.clock
  (:use seesaw.core
        seesaw.graphics
        seesaw.color
        seesaw.test.examples.example))

; A very rudimentary example of (canvas) that draws an analog clock

; Function to create a shape for a hand
(defn hand [length width]
  (path []
    (move-to 0.0       length)
    (line-to width     0.0)
    (line-to 0.0       (- width))
    (line-to (- width) 0.0)
    (line-to 0.0       length)))

; Style for drawing hands
(def hand-style (style :foreground "#999999"
                       :background "#aaaaaa"
                       :stroke (stroke :width 2 :cap :round)))

; Style for the second hand
(def second-style (style :foreground (color 224 0 0 128)
                         :stroke (stroke :width 3 :cap :round)))

; Style for the ticks around the edge of the clock
(def tick-style (style :foreground java.awt.Color/DARK_GRAY
                       :stroke (stroke :width 3 :cap :round)))

(defn second-so-far []

  (let [c (java.util.Calendar/getInstance)]
    (+
      (* (.get c java.util.Calendar/HOUR_OF_DAY) 60 60)
      (* (.get c java.util.Calendar/MINUTE) 60)
      (.get c java.util.Calendar/SECOND))))

(defn paint-clock [^javax.swing.JComponent c ^java.awt.Graphics2D g]
  (let [width       (.getWidth c)
        height  (.getHeight c)
        m       (- (min width height) 15)
        r       (- (/ m 2) 10)
        seconds (second-so-far)
        minutes (/ seconds 60)
        hours   (/ minutes 60)]
    (translate g (/ width 2) (/ height 2))  ; (0, 0) at center of canvas
    (scale g 1 -1) ; flip y
    ; Draw ticks
    (push g
      (dotimes [n 12]
        (rotate g (- (/ 360 12)))
        (draw   g (circle 0 (/ m 2) 3) tick-style)))
    ; Draw minute hand
    (push g
      (rotate g (- (* (/ (mod minutes 60) 60) 360)))
      (draw   g (hand r (/ r 20)) hand-style))
    ; Draw hour hand
    (push g
      (rotate g (- (* (/ hours 12) 360)))
      (draw   g (hand (/ r 1.5) (/ r 20)) hand-style))
    ; Draw second hand
    (push g
      (rotate g (- (* (/ (mod seconds 3600) 60) 360)))
      (draw   g (line 0 0 0 r) second-style))
    ; Draw a little circle in the middle
    (draw g (circle 0 0 3) tick-style)))

(defexample []
  (let [cvs (canvas :id :canvas :background "#BBBBBB" :paint paint-clock)
        t (timer (fn [e] (repaint! cvs)) :delay 1000)]
    (frame
      :title "Seesaw Canvas Clock"
      :width 400 :height 400
      :content cvs)))

;(run :dispose)

