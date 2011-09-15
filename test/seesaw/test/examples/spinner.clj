;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns seesaw.test.examples.spinner
  (:use [seesaw.core]))

(defn app []
  (frame 
    :title "Spinner Example"
    :content
      (border-panel
        :north "This is a spinner with some values"
        :center (spinner :model (map #(str "Value" %) (range 0 100 5))))))


(defn -main [& args]
  (-> (app)
    pack!
    show!))

;(-main)

