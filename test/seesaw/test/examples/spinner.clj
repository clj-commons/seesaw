;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns seesaw.test.examples.spinner
  (:use [seesaw.core]
        seesaw.test.examples.example))

(defexample []
  (frame 
    :title "Spinner Example"
    :content
      (vertical-panel
        :items ["A default spinner (print value change to stdout)"
                (spinner :listen [:selection (fn [e] (println (selection e)))]) 
                "A spinner over a sequence of values"
                (spinner :model (map #(str "Value" %) (range 0 100 5)))
                "An unbounded spinner starting at a particular date"
                (spinner :model (java.util.Date. (long 12345678900)))
                "A numeric spinner starting at a particular value"
                (spinner :model 3.14159)
                "A numeric spinner (spinner-model 3.5 :from 1.5 :to 4.5 :by 0.5)"
                (spinner :model (spinner-model 3.5 :from 1.5 :to 4.5 :by 0.5))
                "A date spinner with explicit start and end"
                (spinner :model (let [s (java.util.Date. (long (* 1000 24 3600 1000)))
                                      v (java.util.Date. (long (* 2000 24 3600 1000)))
                                      e (java.util.Date. (long (* 3000 24 3600 1000))) 
                                      m (spinner-model v :from s :to e :by :day-of-month)]
                                  m))
                ])))


;(run :dispose)

