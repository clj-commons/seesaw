;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.xyz-panel
  (:use [seesaw core border])
  (:import [javax.swing SwingUtilities]))

; Put in some basic dragging support.
(defn draggable [w]
  (listen w
    ; When the mouse is pressed, move the widget to the front of the z order
    :mouse-pressed 
      (fn [e] (move! w :to-front))
    ; When the mouse is dragged move the widget
    :mouse-dragged 
      (fn [e] 
        (let [w (to-widget e)]
        (move! w :to (SwingUtilities/convertPoint w (.getPoint e) (.getParent w))))))
  w)

(defn make-label
  [text]
  (config!
    (label 
      :text       text 
      :location   [(rand-int 300) (rand-int 300)]
      :border     (line-border :thickness 2 :color "#FFFFFF")
      :background "#DDDDDD") 
    ; Set the bounds to its preferred size. Note that this has to be
    ; done after the label is fully constructed.
    :bounds :preferred))

(defn make-panel []
  (xyz-panel 
    :id :xyz 
    :background "#000000"
    :items (conj 
             (map (comp draggable make-label) ["Agent Cooper" "Big Ed" "Leland Palmer"])
             (draggable 
               (config! (border-panel
                          :border (line-border :top 15 :color "#0000BB")
                          :north (label "I'm a draggable label with a text box!")
                          :center (text :text "Hey type some stuff here"))
                :bounds :preferred)))))

(defn -main [& args]
  (invoke-later
    (frame 
      :title "Seesaw xyz-panel example" 
      :content 
        (border-panel
          :north "Demonstration of an xyz-panel with draggable widgets. Try dragging one!"
          :center (make-panel))
      :on-close :exit
      :width 600 :height 600 :pack? false)))

