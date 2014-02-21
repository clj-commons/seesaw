(ns seesaw.test.bind
  (:refer-clojure :exclude [some filter])
  (:require [seesaw.core :as ssc])
  (:use seesaw.bind)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe bind
  (it "returns a composite bindable"
    (let [a (atom 0) b (atom 1) c (atom 2) d (atom 3)
          cb (bind a b c d)
          called (atom nil) ]
      (expect (satisfies? Bindable cb))

      ; make sure that subscribing to the composite subscribes
      ; to the *end* of the chain!
      (subscribe cb (fn [v] (reset! called v)))
      (reset! d 10)
      (expect (= 10 @called))))

  (it "can chain bindables together"
    (let [a (atom 1)
          b (atom nil)]
      (bind a (transform + 5) b)
      (reset! a 5)
      (expect (= 10 @b))))

  (it "returns something function-like that can be called to undo the binding"
    (let [a (atom 0) b (atom 1) c (atom 2)
          cb (bind a b c)]
      (reset! a 5)
      (expect (= [5 5 5] [@a @b @c]))
      (cb)
      (reset! a 6)
      (expect (= [6 5 5] [@a @b @c]))))
  
  (it "can chain bindables, including composites, together"
    (let [a (atom 1)
          b (atom nil)]
      (bind a (bind (transform + 5) (transform * 2) (atom nil)) b)
      (reset! a 2)
      (expect (= 14 @b))))

  (it "should sync the enabled? property of a widget with an atom"
    (let [v (atom true)
          b (ssc/button)]
      (bind (property b :enabled?) v)
      (ssc/config! b :enabled? false)
      (expect (not @v))
      (reset! v true)))

  (it "should sync the enabled? property of a widget with an atom"
    (let [v (atom true)
          b (ssc/button)]
      (bind (property b :enabled?) v)
      (ssc/config! b :enabled? false)
      (expect (not @v))))

  (it "should sync an atom to the enabled? property of a widget"
    (let [v (atom true)
          b (ssc/button)]
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
            t (ssc/text "initial")]
        (bind (.getDocument t) a)
        (ssc/text! t "foo")
        (expect (= "foo" @a))))

    (it "should update the underlying document when the atom changes"
      (let [a (atom "initial")
            t (ssc/text "")]
        (bind a (.getDocument t))
        (reset! a "foo")
        (expect (= "foo" (ssc/text t))))))

  (testing "given a slider"
    (it "should sync the value of the atom with the slider value, if slider value changed"
      (let [v (atom 15)
            sl (ssc/slider :value @v)]
        (bind (.getModel sl) v)
        (.setValue sl 20)
        (expect (= @v 20))))
    (it "should sync the value of the slider with the atom value, if atom value changed"
      (let [v (atom 15)
            sl (ssc/slider :value @v)]
        (bind v (.getModel sl))
        (reset! v 20)
        (expect (= (.getValue sl) 20)))))
 
  (testing "given a toggle button (or any button/menu)"
    (it "should sync the selection state of the button"
      (let [v (atom nil)
            b (ssc/toggle :selected? false)]
        (bind b v)
        (.setSelected b true)
        (expect @v)))

    (it "should sync the selection state of the button"
      (let [v (atom nil)
            b (ssc/toggle :selected? false)]
        (bind v b)
        (reset! v true)
        (expect (.isSelected b)))))

  (testing "given a combobox"
    (it "should sync the selection state of the combobox"
      (let [v (atom nil)
            b (ssc/combobox :model [1 2 3 4])]
        (bind b v)
        (ssc/selection! b 2)
        (expect (= 2 @v))))

    (it "should sync the selection state of the combobox"
      (let [v (atom nil)
            b (ssc/combobox :model [1 2 3 4])]
        (bind v b)
        (reset! v 4)
        (expect (= 4 (ssc/selection b))))))

  (testing "given a spinner"
    (it "should sync the value of the atom with the spinner value, if spinner value changed"
      (let [v (atom 15)
            sl (ssc/spinner :model @v)]
        (bind (.getModel sl) v)
        (.setValue sl 20)
        (expect (= @v 20))))
    (it "should sync the value of the spinner with the atom value, if atom value changed"
      (let [v (atom 15)
            sl (ssc/spinner :model @v)]
        (bind v (.getModel sl))
        (reset! v 20)
        (expect (= (.getValue sl) 20)))))

  (testing "given an agent"

    (it "should pass along changes to the agent's value"
      (let [start (agent nil)
            end   (atom nil)]
        (bind start end)
        (send start (constantly :called))
        (await start)
        (expect (= :called @start))
        (expect (= :called @end))))

    (it "should throw an exception if you try to notify an agent"
      (let [start (atom nil)]
        (bind start (agent nil))
        (expect (try
                  (reset! start 99)
                  false
                  ; In Clojure 1.3, the exception propagates correctly
                  (catch IllegalStateException e
                    true)
                  ; Unfortunately, in Clojure 1.2, IllegalStateException gets wrapped by reset!
                  (catch RuntimeException e
                    (= IllegalStateException (class (.getCause e)))))))))

  (testing "given a Ref"
    (it "should pass along changes to the ref's value"
      (let [start (ref nil)
            end   (atom nil)]
        (bind start end)
        (dosync
         (alter start (constantly "foo")))
        (expect (= "foo" @start))
        (expect (= "foo" @end))))
    (it "should pass update the ref's value when the source changes"
      (let [start (atom nil)
            end   (ref nil)]
        (bind start end)
        (reset! start "foo")
        (expect (= "foo" @start))
        (expect (= "foo" @end)))))        )

(describe b-do*
  (it "executes a function with a single argument and ends a chain"
    (let [start (atom 0)
          called (atom nil) ]
      (bind start (b-do* #(reset! called %)))
      (reset! start 5)
      (expect (= 5 @called)))))

(describe b-do
  (it "executes body with a single argument and ends a chain"
    (let [start (atom [1 2])
          called (atom nil)]
      (bind start (b-do [[a b]] (reset! called (+ a b))))
      (reset! start [3 4])
      (expect (= 7 @called)))))

(describe tee
  (it "creates a tee junction in a bind"
    (let [start (atom 0)
          end1  (atom 0)
          end2  (atom 0)]
      (bind start (tee (bind (transform * 2) end1)
                       (bind (transform * 4) end2)))
      (reset! start 5)
      (expect (= 10 @end1))
      (expect (= 20 @end2)))))

(describe funnel
  (it "create a funnel in a bind which listens to multiple source and produces a vector of values"
    (let [a (atom 0)
          b (atom 1)
          f (funnel a b)
          end (atom nil)]
      (bind f end)
      (reset! a 5)
      (expect (= [5 nil] @end))
      (reset! b 6)
      (expect (= [5 6] @end)))))

(describe filter 
  (it "doesn't pass along value when predicate returns falsey"
    (let [start (atom :foo)
          end   (atom :bar)]
      (bind start (filter (constantly false)) end)
      (reset! start :something)
      (expect (= :bar @end))))
  (it "passes along value when predicate returns truthy"
    (let [start (atom :foo)
          end   (atom :bar)]
      (bind start (filter (constantly true)) end)
      (reset! start :something)
      (expect (= :something @end)))))

(describe some
  (it "doesn't pass along falsey values returned by the predicate"
    (let [start (atom :foo)
          end   (atom :bar)]
      (bind start (some (constantly nil)) end)
      (reset! start :something)
      (expect (= :bar @end))))
  (it "passes along result of predicate when it returns truthy"
    (let [start (atom :foo)
          end   (atom :bar)]
      (bind start (some (constantly :yum)) end)
      (reset! start :something)
      (expect (= :yum @end)))))

(describe selection
  (it "sends out selection changes on a widget"
    (let [lb (ssc/listbox :model [:a :b :c])
          output (atom nil)]
      (bind (selection lb) output)
      (ssc/selection! lb :b)
      (expect (= :b @output))))
  (it "maps its input to the selection of a widget"
    (let [input (atom nil)
          lb (ssc/listbox :model [:a :b :c])]
      (bind input (selection lb))
      (reset! input :b)
      (expect (= :b (ssc/selection lb))))))

(describe value 
  (it "maps its input to the value of a widget"
    (let [input (atom nil)
          lb (ssc/listbox :id :lb :model [:a :b :c])
          tb (ssc/text :id :text)
          p  (ssc/border-panel :north lb :center tb)]
      (bind input (value p))
      (reset! input {:lb :b :text "hi"})
      (expect (= {:lb :b :text "hi"} (ssc/value p))))))

(describe to-bindable
  (it "returns arg if it's already bindable"
    (let [a (atom nil)]
      (expect (= a (to-bindable a)))))
  (it "converts a text component to its document"
    (let [t (ssc/text)]
      (expect (= (.getDocument t) (to-bindable t)))))
  (it "converts a slider to its model"
    (let [s (ssc/slider)]
      (expect (= (.getModel s) (to-bindable s))))))

(describe b-swap!
  (it "acts like swap! passing the old value, new value, and additional args to a function"
    (let [start (atom nil)
          target (atom [])
          end (atom nil)]
      (bind start 
            (b-swap! target conj) 
            end)
      (reset! start 1)
      (reset! start 2)
      (reset! start 3)
      (expect (= [1 2 3] @target))
      (expect (= @end @target)))))

(describe b-send
  (it "acts like send passing the old value, new value, and additional args to a function"
    (let [start  (atom nil)
          target (agent [])]
      (bind start 
            (b-send target conj) )
      (reset! start 1)
      (reset! start 2)
      (reset! start 3)
      (await target)
      (expect (= [1 2 3] @target)))))

(describe b-send-off
  (it "acts like sendoff passing the old value, new value, and additional args to a function"
    (let [start  (atom nil)
          target (agent [])]
      (bind start 
            (b-send-off target conj) )
      (reset! start 1)
      (reset! start 2)
      (reset! start 3)
      (await target)
      (expect (= [1 2 3] @target)))))

(describe notify-later
  (it "passes incoming values to the swing thread with invoke-later"
    (let [start (atom nil)
          end   (atom nil)
          p     (promise)]
      (bind start
            (notify-later)
            (transform (fn [v] {:value v :edt? (javax.swing.SwingUtilities/isEventDispatchThread)}))
            end)
      (subscribe end (fn [v] (deliver p :got-it)))
      (reset! start 99)
      (expect (= :got-it @p))
      (expect (= {:value 99 :edt? true} @end)))))

(describe notify-soon
  (it "passes incoming values to the swing thread with invoke-soon"
    (let [start (atom nil)
          end   (atom nil)]
      (bind start
            (notify-soon)
            (transform (fn [v] {:value v :edt? (javax.swing.SwingUtilities/isEventDispatchThread)}))
            end)
      (ssc/invoke-now (reset! start 99))
      (expect (= {:value 99 :edt? true} @end)))))

(describe notify-now
  (it "passes incoming values to the swing thread with invoke-now"
    (let [start (atom nil)
          end   (atom nil)]
      (bind start
            (notify-now)
            (transform (fn [v] {:value v :edt? (javax.swing.SwingUtilities/isEventDispatchThread)}))
            end)
      (reset! start 99)
      (expect (= {:value 99 :edt? true} @end)))))

(describe subscribe
  (testing "on an atom"
    (it "should return a function that unsubscribes"
      (let [calls (atom 0)
            target (atom "hi")
            unsubscribe (subscribe target (fn [_] (swap! calls inc)))]
        (reset! target "a")
        (expect (= 1 @calls))
        (unsubscribe)
        (reset! target "b")
        (expect (= 1 @calls)))))

  (testing "on an agent"
    (it "should return a function that unsubscribes"
      (let [calls (atom 0)
            target (agent "hi")
            unsubscribe (subscribe target (fn [_] (swap! calls inc)))]
        (await (send target (fn [_] "a")))
        (expect (= 1 @calls))
        (unsubscribe)
        (await (send target (fn [_] "b")))
        (expect (= 1 @calls)))))
  
  (testing "on a javax.swing.text.Document"
    (it "should return a function that unsubscribes"
      (let [calls (atom 0)
            target (.getDocument (ssc/text))
            unsub  (subscribe target (fn [_] (swap! calls inc)))]
        (.insertString target 0 "hi" nil)
        (expect (= 1 @calls))
        (unsub)
        (.insertString target 0 "bye" nil)
        (expect (= 1 @calls))
        )))
  (testing "on a javax.swing.BoundedRangeModel"
    (it "should return a function that unsubscribes"
      (let [calls (atom 0)
            target (javax.swing.DefaultBoundedRangeModel. 50 0 2 100)
            unsub  (subscribe target (fn [_] (swap! calls inc)))]
        (.setValue target 1)
        (expect (= 1 @calls))
        (unsub)
        (.setValue target 2)
        (expect (= 1 @calls)))))
  
  (testing "on a funnel"
    (it "should return a function that unsubscribes"
      (let [calls  (atom 0)
            a      (atom "hi")
            target (funnel a)
            unsub  (subscribe target (fn [_] (swap! calls inc)))]
        (reset! a "bye")
        (expect (= 1 @calls))
        (unsub)
        (reset! a "hi")
        (expect (= 1 @calls))
        )))
  
  (testing "on a widget property"
    (it "should return a function that unsubscribes"
      (let [calls  (atom 0)
            w      (ssc/text)
            target (property w :enabled?)
            unsub  (subscribe target (fn [_] (swap! calls inc)))]
        (.setEnabled w false)
        (expect (= 1 @calls))
        (unsub)
        (.setEnabled w true)
        (expect (= 1 @calls))
        )))
  (testing "on a transform"
    (it "should return a function that unsubscribes"
      (let [calls  (atom 0)
            target (transform identity)
            unsub  (subscribe target (fn [_] (swap! calls inc)))]
        (notify target "hi")
        (expect (= 1 @calls))
        (unsub)
        (notify target "bye")
        (expect (= 1 @calls))
        )))
  (testing "on a filter"
    (it "should return a function that unsubscribes"
      (let [calls  (atom 0)
            target (filter (constantly true))
            unsub  (subscribe target (fn [_] (swap! calls inc)))]
        (notify target "hi")
        (expect (= 1 @calls))
        (unsub)
        (notify target "bye")
        (expect (= 1 @calls)))))
  (testing "on a some"
    (it "should return a function that unsubscribes"
      (let [calls  (atom 0)
            target (some (constantly true))
            unsub  (subscribe target (fn [_] (swap! calls inc)))]
        (notify target "hi")
        (expect (= 1 @calls))
        (unsub)
        (notify target "bye")
        (expect (= 1 @calls))
        ))))

