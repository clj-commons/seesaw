;  Copyright (c) Dave Ray, Meikel Brandmeyer 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.forms
  (:use [seesaw [core :exclude (separator)] forms]
        seesaw.test.examples.example))

; Example similar to miglayout

(defn frame-content
  []
  (forms-panel
    "pref,4dlu,80dlu,8dlu,pref,4dlu,80dlu"
    :column-groups [[1 5]]
    :items [(separator "General")
            "Company" (span (text) 5)
            "Contact" (span (text) 5)
            (separator "Propeller")
            "PTI/kW"  (text :columns 10) "Power/kW" (text :columns 10)
            "R/mm"    (text :columns 10) "D/mm"     (text :columns 10)]
    :default-dialog-border? true))

(defexample []
  (frame :title "jGoodies FormLayout Example"
         :resizable? false
         :content (frame-content)))

;(run :dispose)

