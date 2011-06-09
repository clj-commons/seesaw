;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.behave
  (:use seesaw.behave)
  (:use seesaw.core)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe when-focused-select-all
  (it "causes all text in a text field to be selected when it gains focus"
    (let [t (text "Hi there")
          remove-fn (when-focused-select-all t)]
      ; Simulate focus gained :(
      (doseq [l (.getFocusListeners t)]
        (.focusGained l (java.awt.event.FocusEvent. t java.awt.event.FocusEvent/FOCUS_GAINED)))
      (expect (= "Hi there" (.getSelectedText t))))))

