;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.xyz-panel
  (:use [seesaw core border behave])
  (:import [javax.swing SwingUtilities]))


; Put in some basic support for moving w around.
(defn movable [w]
  (when-mouse-dragged w
    ; When the mouse is pressed, move the widget to the front of the z order
    :start #(move! % :to-front)
    ; When the mouse is dragged move the widget
    :drag  #(move! %1 :by %2))
  w)

(defn make-label
  [text]
  (doto (label 
          :text       text 
          :location   [(rand-int 300) (rand-int 300)]
          :border     (line-border :thickness 2 :color "#FFFFFF")
          :background "#DDDDDD")
    ; Set the bounds to its preferred size. Note that this has to be
    ; done after the label is fully constructed.
      (config! :bounds :preferred)))

(defn make-panel []
  (xyz-panel 
    :id :xyz 
    :background "#000000"
    :items (conj 
             (map (comp movable make-label) ["Agent Cooper" "Big Ed" "Leland Palmer"])
             (doto (border-panel
                       :border (line-border :top 15 :color "#0000BB")
                       :north (label "I'm a draggable label with a text box!")
                       :center (text :text "Hey type some stuff here"))
                   (config! :bounds :preferred)
                   movable))))

(defn -main [& args]
  (invoke-later
    (show!
      (frame 
        :title "Seesaw xyz-panel example" 
        :content 
          (border-panel
            :north "Demonstration of an xyz-panel with draggable widgets. Try dragging one!"
            :center (make-panel))
        :width 600 :height 600))))
(-main)

