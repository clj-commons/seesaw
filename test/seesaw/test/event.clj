(ns seesaw.test.event
  (:use seesaw.event)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [javax.swing.event ChangeListener]
           [java.awt.event ItemListener MouseListener MouseMotionListener]))

(defn verify-empty-listener
  [listener-type handler-key dispatch-fn]
  (let [listener (make-listener listener-type (ref {}))]
    (dispatch-fn listener)
    true))

(defn verify-listener
  [listener-type handler-key dispatch-fn]
  (let [called (atom false)
        handler (fn [e] (reset! called true))
        listener (make-listener listener-type (ref { handler-key [handler] }))]
    (dispatch-fn listener)
    (or @called 
        (throw (RuntimeException. (str listener-type ", " handler-key " wasn't called!"))))))

(defn verify-listeners
  [listener-type & args]
  (every? true? (map (fn [[hk df]] (verify-listener listener-type hk df)) (partition 2 args))))

(describe add-listener
  (it "inserts necessary keys and creates an initial list"
    (let [listener  #(println %)]
      (expect (= {:test-key [listener]} (add-listener {} :test-key listener)))))
  (it "can insert additional listeners"
    (let [listener  #(println %)]
      (expect (= {:test-key [:dummy listener]} 
         (add-listener {:test-key [:dummy]} :test-key listener))))))

(describe remove-listener
  (it "can remove a listener"
    (let [initial {:list-key [:a :b :c]}
          result  (remove-listener initial :list-key :b)
          _       (println "RESULT: " (:list-key result))]
      (expect (= {:list-key [:a :c]} result)))))


(describe make-listener
  (testing "for ChangeListener"
    (it "instantiates a ChangeListener instance"
      (instance? ChangeListener (make-listener ChangeListener (ref {}))))
    (it "makes an instance that does nothing when there's no handler"
      (verify-empty-listener ChangeListener :state-changed #(.stateChanged % nil)))
    (it "makes an instance that calls :state-changed"
      (verify-listener ChangeListener :state-changed #(.stateChanged % nil))))

  (testing "for ItemListener"
    (it "instantiates an ItemListener instance"
      (instance? ItemListener (make-listener ItemListener (ref {}))))
    (it "makes an instance that does nothing when there's no handler"
      (verify-empty-listener ItemListener :item-state-changed #(.itemStateChanged % nil)))
    (it "makes an instance that calls :item-state-changed"
      (verify-listener ItemListener :item-state-changed #(.itemStateChanged % nil))))
          
  (testing "for MouseListener"
    (it "instantiates an MouseListener instance"
      (instance? MouseListener (make-listener MouseListener (ref {}))))
    (it "makes an instance that does nothing when there's no handlers"
      (verify-empty-listener MouseListener :mouse-clicked #(.mouseClicked % nil)))
    (it "makes an instance that calls expected methods"
      (verify-listeners MouseListener 
        :mouse-clicked #(.mouseClicked % nil)
        :mouse-entered #(.mouseEntered % nil)
        :mouse-exited #(.mouseExited % nil)
        :mouse-pressed #(.mousePressed % nil)
        :mouse-released #(.mouseReleased % nil))))

  (testing "for MouseMotionListener"
    (it "instantiates an MouseMotionListener instance"
      (instance? MouseMotionListener (make-listener MouseMotionListener (ref {}))))
    (it "makes an instance that does nothing when there's no handlers"
      (verify-empty-listener MouseMotionListener :mouse-moved #(.mouseMoved % nil)))
    (it "makes an instance that calls expected methods"
      (verify-listeners MouseMotionListener 
        :mouse-moved #(.mouseMoved % nil)
        :mouse-dragged #(.mouseDragged % nil)))))

