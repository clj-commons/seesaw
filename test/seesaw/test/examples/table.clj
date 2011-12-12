;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.table
  (:use [seesaw core table]
        seesaw.test.examples.example))

; A simple example of (table) for basic tabular data

(defn make-table []
  (table :id :table
    :model [
      :columns [ { :key :name :text "Name" } 
                 { :key :town :text "Town" } 
                 { :key :interest :text "Interest" }]
      :rows [{ :name "Kupzog" :town "Cologne" :interest "programming" :id 1234}
             { :name "Hansson" :town "Ystadt" :interest "Hunting" :id 2234}
             { :name "Walter" :town "London" :interest "Rafting" :id 12345}]]))

; Note that the :id key isn't in the :columns spec, but it is still retained
; behind the scenes by the table model.

; The rest is boilerplate ...
(defn make-frame []
  (frame :title "JTable Example" :width 500 :height 400 :content
    (border-panel
      :center (scrollable (make-table))
      :south  (label :id :sel :text "Selection: "))))

(defexample []
  (let [f (show! (make-frame))
        t (select f [:#table])]
    ; Listen for selection changes and show them in the label
    (listen t :selection 
      (fn [e] 
        (config! (select f [:#sel]) 
          :text (str "Selection: " 
                     ; (selection t) returns the selected row index
                     ; (value-at t row) returns the record at row
                     (value-at t (selection t))))))
    f))

;(run :dispose)

