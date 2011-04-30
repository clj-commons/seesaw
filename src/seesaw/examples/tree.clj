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

(def data '((1 2 (3)) (4) 5 (6 (7 8 9 10) 11)))

(defn app []
  (frame :title "JTree Example" :content
    (border-panel
      :center (scrollable (tree :model (simple-tree-model seq? seq data))))))

(defn -main [& args]
  (invoke-later (app)))

(-main)
