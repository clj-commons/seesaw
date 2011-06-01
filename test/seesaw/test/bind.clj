(ns seesaw.test.bind
  (:use [seesaw.bind])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe bind-atom-to-range-model
  (it "Updates an atom when the model changes"
    (let [a (atom -1)
          m (javax.swing.DefaultBoundedRangeModel. 50 0 2 100)]
      (expect (= a (bind-atom-to-range-model a m)))
      (expect (= 50 @a))
      (.setValue m 51)
      (expect (= 51 @a))))
  (it "Updates the model when the atom changes"
    (let [a (atom -1)
          m (javax.swing.DefaultBoundedRangeModel. 50 0 2 100)]
      (expect (= a (bind-atom-to-range-model a m)))
      (reset! a 99)
      (expect (= 99 (.getValue m))))))

