;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.slider
  (:use [seesaw core color border]
        seesaw.test.examples.example))

(defn make-frame []
  (frame 
    :title "Slider Example"
    :content
      (horizontal-panel :items [
        (vertical-panel :items [
          "<html>
          Slide the sliders to change<br>
          the color to the right</html>"
          (slider :id :red   :min 0 :max 255)
          (slider :id :green :min 0 :max 255)
          (slider :id :blue  :min 0 :max 255)])
        (canvas :id :canvas :border (line-border) :size [200 :by 200])])))

(defn update-color [root]
  (let [{:keys [red green blue]} (value root)] ; <- Use (value) to get map of values
    (config! (select root [:#canvas]) 
             :background (color red green blue))))

(defexample []
  (let [root (make-frame)]
    (listen (map #(select root [%]) [:#red :#green :#blue]) :change
            (fn [e]
              (update-color root)))
    root))

;(run :dispose)

