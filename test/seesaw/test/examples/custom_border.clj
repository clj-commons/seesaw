;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.custom-border
  (:use [seesaw.core :only [frame label show!]]
        [seesaw.border :only [custom-border]]
        seesaw.test.examples.example))

(defexample []
  (frame 
    :size [400 :by 400]
    :content (label :text "I have a custom border"
                    :border (custom-border :insets 10 
                                           :paint (fn [c g x y w h]
                                                    (doto g
                                                      (.setColor java.awt.Color/RED)
                                                      (.drawRoundRect (+ 5 x) (+ 5 y) (- w 10) (- h 10) 15 15)))))))

;(run :dispose)

