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

(defn draggable [w]
  (listen w
    :mouse-dragged 
      (fn [e] 
        (let [w (to-widget e)]
        (move! w :to (SwingUtilities/convertPoint w (.getPoint e) (.getParent w))))))
  w)

(defn resize [w]
  (let [ps (.getPreferredSize w)]
    (config! w :bounds [0 0 (.width ps) (.height ps)])))

(defn make-label
  [text]
  (label 
    :text text 
    :border (line-border :thickness 2 :color "#FFFFFF")
    :bounds [0 0 50 20] 
    :background "#DDDDDD"))

(defn make-panel []
  (xyz-panel 
    :id :xyz 
    :background "#000000"
    :items (map (comp draggable resize make-label) ["Agent Cooper" "Big Ed" "Leland Palmer"])))

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

