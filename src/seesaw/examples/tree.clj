;  Copyright (c) Dave Ray, 2011. All ritest/seesaw/test/core.clj

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.tree
  (:use [seesaw core tree]))

; Some tree-like data
(def data '((1 2 (3)) (4) 5 (6 (7 8 9 10) 11)))

; Make a tree model for the data using same functions as (clojure.core/tree-seq)
(def model (simple-tree-model seq? seq data))

; A custom renderer so that list nodes are displayed as lists.
(defn render-fn [renderer info]
  (let [v (:value info)]
    (config renderer 
      :text (if (seq? v) 
              (format "List (%d items)" (count v)) 
              v))))

; The rest is boilerplate ...
(defn app []
  (frame :title "JTree Example" :content
    (border-panel
      :center (scrollable (tree :id :tree
                                :model model
                                :renderer render-fn))
      :south (label :id :sel :text "Selection: ")))

  ; Listen for selection changes and show them in the label
  (listen (select [:#tree]) :selection 
    (fn [e] 
      (config (select [:#sel]) 
        :text (str "Selection: " (-> (to-widget e) selection first last))))))

(defn -main [& args]
  (invoke-later (app)))

(-main)

