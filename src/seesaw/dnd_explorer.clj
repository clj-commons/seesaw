;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.dnd-explorer
  (:use [seesaw core dnd])
  (:require [seesaw.table :as table]))

(defn drop-handler [t support]
  (let [flavors (.getDataFlavors support)] 
    (table/clear! t)
    (apply table/insert-at! t 
          (mapcat 
            (fn [f i] [0 (assoc (bean f) :N (format "%04d" i))])
            flavors
            (iterate inc 0)))))

(defn app []
  (let [t (doto (table
            :show-grid? true
            :model [:columns [:N
                              :representationClass
                              :primaryType
                              :subType
                              :humanPresentableName]])
            (.setAutoCreateRowSorter true))] 
    (frame
      :title "Seesaw Drag-n-Drop Explorer"
      :size [640 :by 480]
      :content (border-panel
                :north (text
                        :text "Drop stuff here. Flavors shown below."
                        :background :lightblue
                        :font "Arial-BOLD-20"
                        :editable? false
                        :drop-mode :insert
                        :transfer-handler (everything-transfer-handler (partial drop-handler t)))
                :center (scrollable t)))))

(defn -main [& args]
  (invoke-later
    (-> (app)
      show!)))

;(-main)

