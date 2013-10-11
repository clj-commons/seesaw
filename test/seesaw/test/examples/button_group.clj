;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.button-group
  (:use seesaw.core
        seesaw.test.examples.example))

; An example of putting radio buttons (or toggle buttons, or menu items, etc)
; in a button group to ensure mutual exclusion. Note that the group is a
; logical concept. It is not displayed anywhere.

(defn content
  []
  ; Make a button group
  (let [group (button-group)
        info-label (label :text "Info goes here" :border 10)
        panel (border-panel
                :north
                  (horizontal-panel
                    ; Put radio buttons in the group with the :group option
                    ; Alternatively, the buttons could be created first and
                    ; then passed to the :buttons option of (button-group)
                    :items [(radio :text "First" :group group)
                            (radio :text "Second" :group group)
                            (radio :text "Third" :selected? true :group group)])
                :center info-label)]
    ; This will hook an action listener to all the buttons in the group
    ; Note that if more buttons are added, this is *not* extended to those
    ; new buttons.
    (listen group :action
      (fn [e]
        (text! info-label
          ; Which button was selected can be retrieved either through
          ; the event object in the usual way, or by asking for the
          ; buton group's selection.
          (str
            "<html>"
            "(text (to-widget e)): " (text (to-widget e)) "<br>"
            "<br>"
            "(text (selection group)): " (text (selection group)) "</html>"))))
    panel))

(defexample run []

  (frame :title "Seesaw Button Group Example"
         :height 150
         :width 300
         :content (content)))

;(run :dispose)

