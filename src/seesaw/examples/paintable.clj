;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.paintable
  (:use [seesaw core graphics]))

(defn draw-a-red-x 
  "Draw a red X on a widget with the given graphics context"
  [c g]
  (let [w          (width c)
        h          (height c)
        line-style (style :foreground "#FF0000" :stroke 3 :cap :round)
        d 5]
    (draw g
      (line d d (- w d) (- h d)) line-style
      (line (- w d) d d (- h d)) line-style)))

(defn content []
  (flow-panel 
    :border 5
    :items [
      (label           :text "I'm a good label!" :font "ARIAL-BOLD-40" :foreground "#00AA00")
      (paintable label :text "I'm a bad label!"  :font "ARIAL-BOLD-40" :paint draw-a-red-x)
      (paintable button :text "I'm a bad button!"  :font "ARIAL-BOLD-40" :paint draw-a-red-x)]))

(defn -main [& args]
  (invoke-later
    (->
      (frame :title "Seesaw (paintable) example"
             :content (content))
      pack!
      show!)))

(-main)

