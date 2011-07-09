(ns seesaw.test.bind
  (:use seesaw.core 
        seesaw.bind)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe bind
  (it "can chain bindables together"
    (let [a (atom 1)
          b (atom nil)]
      (bind a (transform + 5) b)
      (reset! a 5)
      (expect (= 10 @b))))

  (it "should sync the enabled? property of a widget with an atom"
    (let [v (atom true)
          b (button)]
      (bind (property b :enabled?) v)
      (config! b :enabled? false)
      (expect (not @v))
      (reset! v true)))

  (it "should sync the enabled? property of a widget with an atom"
    (let [v (atom true)
          b (button)]
      (bind (property b :enabled?) v)
      (config! b :enabled? false)
      (expect (not @v))))

  (it "should sync an atom to the enabled? property of a widget"
    (let [v (atom true)
          b (button)]
      (bind v (property b :enabled?))
      (reset! v false)
      (expect (not (.isEnabled b)))))

  (testing "with a BoundedRangeModel"
    (it "Updates an atom when the model changes"
      (let [a (atom -1)
            m (javax.swing.DefaultBoundedRangeModel. 50 0 2 100)]
        (bind m a)
        (.setValue m 51)
        (expect (= 51 @a))))
    (it "Updates the model when the atom changes"
      (let [a (atom -1)
            m (javax.swing.DefaultBoundedRangeModel. 50 0 2 100)]
        (bind a m)
        (reset! a 99)
        (expect (= 99 (.getValue m))))))

  (testing "given a text field"
    (it "should update an atom when the underlying document changes"
      (let [a (atom nil)
            t (text "initial")]
        (bind (.getDocument t) a)
        (text! t "foo")
        (expect (= "foo" @a))))

    (it "should update the underlying document when the atom changes"
      (let [a (atom "initial")
            t (text "")]
        (bind a (.getDocument t))
        (reset! a "foo")
        (expect (= "foo" (text t))))))

  (testing "given a slider"
    (it "should sync the value of the atom with the slider value, if slider value changed"
      (let [v (atom 15)
            sl (slider :value @v)]
        (bind (.getModel sl) v)
        (.setValue sl 20)
        (expect (= @v 20))))
    (it "should sync the value of the slider with the atom value, if atom value changed"
      (let [v (atom 15)
            sl (slider :value @v)]
        (bind v (.getModel sl))
        (reset! v 20)
        (expect (= (.getValue sl) 20))))))

