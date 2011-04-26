;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.canvas
  (:use seesaw.core)
  (:use seesaw.color))

; A very rudimentary example of (canvas).


; Define some paint handlers. Each takes the canvas and Graphics2D object
; as args. The function is called within a (seesaw.graphics/push) block
; so any changes made to the graphics context will be backed out when
; the function returns.
;
; Note that this code is pretty lame because there are currently
; no wrappers for any Java2D stuff. Way it goes.
(defn paint1 [c g]
  (let [w (.getWidth c)
        h (.getHeight c)]
    (doto g
      (.setColor (color 224 224 0 128))
      (.fillRect 0 0 (/ w 2) (/ h 2))
      (.setColor (color 0 224 224 128))
      (.fillRect 0 (/ h 2) (/ w 2) (/ h 2))
      (.setColor (color 224 0 224 128))
      (.fillRect (/ w 2) 0 (/ w 2) (/ h 2))
      (.setColor (color 224 0 0 128))
      (.fillRect (/ w 2) (/ h 2) (/ w 2) (/ h 2))
      (.setColor (color 0 0 0))
      (.drawString "Hello. This is a canvas example" 20 20))))

(defn paint2 [c g]
  (let [w (.getWidth c)
        h (.getHeight c)]
    (doto g
      (.setColor (color 224 224 0 128))
      (.fillOval 0 0 (/ w 2) (/ h 2))
      (.setColor (color 0 224 224 128))
      (.fillOval 0 (/ h 2) (/ w 2) (/ h 2))
      (.setColor (color 224 0 224 128))
      (.fillOval (/ w 2) 0 (/ w 2) (/ h 2))
      (.setColor (color 224 0 0 128))
      (.fillOval (/ w 2) (/ h 2) (/ w 2) (/ h 2))
      (.setColor (color 0 0 0))
      (.drawString "Hello. This is a canvas example" 20 20))))

; Create an action that swaps the paint handler for the canvas.
; Note that we can use (config) to set the :paint handler just like
; properties on other widgets.
(defn switch-paint-action [n paint]
  (action :name n 
          :handler (fn [e] (config (select :#canvas) :paint paint))))

(defn app
  []
  (frame :title "Canvas Example" :width 500 :height 300 :pack? false
    :content 
      (border-panel :hgap 5 :vgap 5 :border 5
        ; Create the canvas with initial nil paint function, i.e. just canvas
        ; will be filled with it's background color and that's it.
        :center (canvas :id :canvas :background "#BBBBDD" :paint nil)

        ; Some buttons to swap the paint function
        :south (horizontal-panel :items ["Switch canvas paint function: "
                                        (switch-paint-action "None" nil)
                                        (switch-paint-action "Rectangles" paint1)
                                        (switch-paint-action "Ovals" paint2)]))))

(defn -main [& args]
  (invoke-later (app)))
;(-main)

