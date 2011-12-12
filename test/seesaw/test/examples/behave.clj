;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.behave
  (:use [seesaw core behave]
        seesaw.test.examples.example))

; Examples of stuff in (seesaw.behave). 

(defn focus-select []
  (vertical-panel
    :border "Demonstrates use of when-focused-select-all"
    :items [
      (doto
        (text "All this will be selected when I get focus")
        when-focused-select-all)
      :separator
      (doto
        (combobox :editable? true :model ["Same here. Hit tab!" "First" "Second" "Third"])
        when-focused-select-all)]))

(defexample []
  (frame :title "seesaw.behave examples" :content (focus-select)))

;(run :dispose)

