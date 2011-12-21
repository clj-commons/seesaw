;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.kotka-bind
  (:require [seesaw.bind :as bind])
  (:use [seesaw.core]
        seesaw.test.examples.example))

; seesaw.bind applied to http://kotka.de/blog/2010/05/Decoupling_Logic_and_GUI.html
;
; shows how to bind atom values to widge properties to decouple logic from
; display.

(defn processing
  [items done? canceled? progress]
  (doseq [item (take-while (fn [_] (not @canceled?)) items)]
    (println item)
    (Thread/sleep 50)
    (swap! progress inc))
  (reset! done? true))

(defexample []
  (let [items     (take 500 (iterate inc 0))
        progress  (atom 0)
        done?     (atom false)
        canceled? (atom false)
        pbar      (progress-bar :value 0 :max (count items))
        done      (button :text "Done" :enabled? false)
        cancel    (button :text "Cancel")
        panel     (border-panel
                    :border 5 :hgap 5 :vgap 5
                    :north "The progress bar updates 20 times per second"
                    :center pbar
                    :south (grid-panel :rows 1 :items [done cancel]))]
    ; Wire up the done button
    (listen done :action-performed dispose!)
    ; Toggle buttons on done
    (bind/bind 
      done? 
      (bind/some identity) ; (when done?)
      (bind/notify-later)  ; cross to swing thread
      (bind/tee
        (bind/property done :enabled?)
        (bind/bind 
          (bind/transform not) 
          (bind/property cancel :enabled?))))
    ; Wire up cancel button.
    (listen cancel :action-performed 
      (fn [_] 
        (reset! canceled? true)))
    ; Wire up progress bar.
    (bind/bind 
      progress 
      (bind/notify-later) 
      pbar)
    (-> #(processing items done? canceled? progress) Thread. .start)
    (frame :title "Example GUI" :on-close :dispose :content panel)))

;(run :dispose)

