;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.table
  (:use [seesaw core]))

; A simple example of (table) for basic tabular data

(defn make-table []
  (table :id :table
    :model [
      :columns [ { :key :name :text "Name" } 
                 { :key :town :text "Town" } 
                 { :key :interest :text "Interest" }]
      :rows [{ :name "Kupzog" :town "Cologne" :interest "programming"}
             { :name "Hansson" :town "Ystadt" :interest "Hunting"}
             { :name "Walter" :town "London" :interest "Rafting"}]]))

; The rest is boilerplate ...
(defn make-frame []
  (frame :title "JTable Example" :width 400 :height 400 :pack? false :content
    (border-panel
      :center (scrollable (make-table))
      :south  (label :id :sel :text "Selection: "))))

(defn get-selected-name [t]
  (let [row (selection t)]
    ; TODO This is super lame.
    (.getValueAt (.getModel t) row 0)))

(defn app []
  (let [f (make-frame)
        t (select f [:#table])]
    ; Listen for selection changes and show them in the label
    (listen t :selection 
      (fn [e] 
        (config! (select f [:#sel]) 
          :text (str "Selection: " (get-selected-name t)))))))

(defn -main [& args]
  (invoke-later (app)))

;(-main)

