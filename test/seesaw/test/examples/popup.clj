;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns seesaw.test.examples.popup
  (:use [seesaw core border]
        seesaw.test.examples.example))

; An example of adding a popup (i.e. right-click/context) menu to a component.

; A static popup menu with fixed items
(def static-popup (popup :items ["HI" "BYE"]))

; A function that takes a mouse event and dynamically generates the items
; of the displayed popup menu
(defn dynamic-popup
  [event]
  [(str "HI-" (rand)) "BYE"])

(defexample []
  (frame :title "Popup Menu Example" :width 500 :height 300
    :content
      (left-right-split
        ; Just a couple of labels with popup menus associated

        (label :text "<html>Right Click Me<br>(static popup menu)</html>"
               :border [5 (line-border)]
               :popup static-popup)

        (label :text "<html>No, Right Click Me!<br>(dynamically popuplated menu)</html>"
               :border [5 (line-border)]
               :popup dynamic-popup)
        :divider-location 1/2)))

;(run :dispose)

