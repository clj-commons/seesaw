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

(defn when-mouse-dragged 
  "A helper for handling mouse dragging on a widget. This isn't that complicated,
  but the default mouse dragged event provided with Swing doesn't give the delta
  since the last drag event so you end up having to keep track of it. This function
  takes three options:

    :start event handler called when the drag is started (mouse pressed).
    :drag  A function that takes a mouse event and a [dx dy] vector which is
           the change in x and y since the last drag event.
    :finish event handler called when the drag is finished (mouse released).

  Like (seesaw.core/listen) returns a function which will remove all event handlers
  when called.

  Examples:
    

  See:

  "
  [w & opts]
  (let [{:keys [start drag finish] 
         :or   { start (fn [e]) drag (fn [e]) finish (fn [e]) }} opts
        last-point (java.awt.Point.)]
    (listen w
      :mouse-pressed 
        (fn [e] 
          (.setLocation last-point (.getPoint e))
          (start e))
      :mouse-dragged 
        (fn [e]
          (let [p (.getPoint e)]
            (drag e [(- (.x p) (.x last-point)) (- (.y p) (.y last-point))])))
      :mouse-released
        finish)))

; Put in some basic dragging support.
(defn draggable [w]
  (when-mouse-dragged w
    ; When the mouse is pressed, move the widget to the front of the z order
    :start (fn [e] (move! w :to-front))
    ; When the mouse is dragged move the widget
    :drag
      (fn [e delta] (move! w :by delta)))
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
    (show!
      (frame 
        :title "Seesaw xyz-panel example" 
        :content 
          (border-panel
            :north "Demonstration of an xyz-panel with draggable widgets. Try dragging one!"
            :center (make-panel))
        :width 600 :height 600))))
;(-main)

