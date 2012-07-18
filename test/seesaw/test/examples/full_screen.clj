;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.full-screen
  (:use [seesaw core]
        seesaw.test.examples.example))

; Example usage of toggle-full-screen!. In this example, the frame is undecorated
; so we have to provide an explicit close button.

(defn make-frame []
  (frame :title  "Seesaw Full-screen Example"
         :width  400
         :height 400
         ;:undecorated? true
         :content (vertical-panel 
                    :items ["A demo of (seesaw.core/toggle-full-screen!)."
                            (button :id :toggle :text "Toggle full screen")
                            (button :id :close :text "Close")])))

(defn add-behaviors [root]
  (listen (select root [:#toggle]) :action (fn [e] (toggle-full-screen! root)))
  (listen (select root [:#close]) :action (fn [e] (dispose! root)))
  root)

(defexample []
  (-> (make-frame)
    add-behaviors
    show!))

;(run :dispose)

