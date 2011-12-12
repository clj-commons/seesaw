;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.tree
  (:use [seesaw core tree]
        seesaw.test.examples.example)
  (:require clojure.xml))

(def source "http://www.4clojure.com/problems/rss")

; Make a tree model for some XML using same arguments (branch? and childnre) 
; as (clojure.core/tree-seq)
(defn load-model []
  (simple-tree-model 
    (complement string?) 
    (comp seq :content)
    (clojure.xml/parse source)))

; A custom renderer so that XML elements are displayed nicely
(defn render-fn [renderer info]
  (let [v (:value info)]
    (config! renderer 
      :text (if (map? v) 
              (format "<%s>" (name (:tag v)))
              v))))

; The rest is boilerplate ...
(defn make-frame []
  (frame :title "JTree Example" :width 400 :height 400 :content
    (border-panel
      :north  (str "From: " source)
      :center (scrollable (tree :id :tree
                                :model (load-model)
                                :renderer render-fn))
      :south  (label :id :sel :text "Selection: "))))

(defexample []
  (let [f (make-frame)]
    ; Listen for selection changes and show them in the label
    (listen (select f [:#tree]) 
      :tree-will-expand #(println (str "Tree will expand\n" %))
      :tree-will-collapse #(println (str "Tree will collapse\n" %))
      :tree-expanded #(println (str "Tree expanded\n" %))
      :tree-collapsed #(println (str "Tree collapsed\n" %))
      :selection 
        (fn [e] 
          (config! (select f [:#sel]) 
            :text (str "Selection: " (-> e selection first last)))))
    f))

;(run :dispose)

