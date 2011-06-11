;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.xyz-panel
  (:use [seesaw core border behave graphics])
  (:import [javax.swing SwingUtilities]))


; Put in some basic support for moving w around using behave/when-mouse-dragged.
(defn movable [w]
  (when-mouse-dragged w
    ; When the mouse is pressed, move the widget to the front of the z order
    :start #(move! % :to-front)
    ; When the mouse is dragged move the widget
    :drag  #(move! %1 :by %2))
  w)

(defn make-label
  [text]
  (doto 
    ; Instead of a boring label, make the label rounded with
    ; some custom drawing. Use the before paint hook to draw
    ; under the label's text.
    (paintable label 
      :border   5
      :text     text 
      :location [(rand-int 300) (rand-int 300)]
      :paint { 
        :before (fn [c g]
                  (draw g (rounded-rect 3 3 (- (.getWidth c) 6) (- (.getHeight c) 6) 9)
                          (style :foreground "#FFFFaa"
                                  :background "#aaFFFF"
                                  :stroke 2)))})
    ; Set the bounds to its preferred size. Note that this has to be
    ; done after the label is fully constructed.
    (config! :bounds :preferred)))

(defn draw-grid [c g]
  (let [w (.getWidth c) 
        h (.getHeight c)]
    (doseq [x (range 0 w 10)]
      (.drawLine g x 0 x h))
    (doseq [y (range 0 h 10)]
      (.drawLine g 0 y w y))))

(defn make-panel []
  (let [items (conj 
             (map (comp movable make-label) ["Agent Cooper" "Big Ed" "Leland Palmer"])
             (doto (border-panel
                       :border (line-border :top 15 :color "#AAFFFF")
                       :north (label "I'm a draggable label with a text box!")
                       :center (text :text "Hey type some stuff here"))
                   (config! :bounds :preferred)
                   movable))]
  (paintable
    xyz-panel 
    :paint draw-grid
    :id :xyz 
    :background "#222222"
    :items items)))

(defn -main [& args]
  (invoke-later
    (show!
      (frame 
        :title   "Seesaw xyz-panel example" 
        :content (border-panel
                   :vgap 5
                   :north "Demonstration of an xyz-panel with draggable widgets. Try dragging one!"
                   :center (make-panel))
        :size    [600 :by 600]))))
;(-main)

