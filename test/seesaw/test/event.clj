(ns seesaw.test.event
  (:use seesaw.event)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [javax.swing JPanel JTextField JButton]
           [javax.swing.event ChangeListener]
           [java.awt.event ItemListener MouseListener MouseMotionListener]))

(defn verify-empty-listener
  [listener-type handler-key dispatch-fn]
  (let [listener (reify-listener listener-type (ref {}))]
    (dispatch-fn listener)
    true))

(defn verify-listener
  [listener-type handler-key dispatch-fn]
  (let [called (atom false)
        handler (fn [e] (reset! called true))
        listener (reify-listener listener-type (ref { handler-key [handler] }))]
    (dispatch-fn listener)
    (or @called 
        (throw (RuntimeException. (str listener-type ", " handler-key " wasn't called!"))))))

(defn verify-listeners
  [listener-type & args]
  (every? true? (map (fn [[hk df]] (verify-listener listener-type hk df)) (partition 2 args))))

(describe append-listener
  (it "inserts necessary keys and creates an initial list"
    (let [listener  #(println %)]
      (expect (= {:test-key [listener]} (append-listener {} :test-key listener)))))
  (it "can insert additional listeners"
    (let [listener  #(println %)]
      (expect (= {:test-key [:dummy listener]} 
         (append-listener {:test-key [:dummy]} :test-key listener))))))

(describe unappend-listener
  (it "can remove a listener"
    (let [initial {:list-key [:a :b :c]}
          result  (unappend-listener initial :list-key :b)
          _       (println "RESULT: " (:list-key result))]
      (expect (= {:list-key [:a :c]} result)))))


(describe reify-listener
  (testing "for ChangeListener"
    (it "instantiates a ChangeListener instance"
      (instance? ChangeListener (reify-listener ChangeListener (ref {}))))
    (it "makes an instance that does nothing when there's no handler"
      (verify-empty-listener ChangeListener :state-changed #(.stateChanged % nil)))
    (it "makes an instance that calls :state-changed"
      (verify-listener ChangeListener :state-changed #(.stateChanged % nil))))

  (testing "for ItemListener"
    (it "instantiates an ItemListener instance"
      (instance? ItemListener (reify-listener ItemListener (ref {}))))
    (it "makes an instance that does nothing when there's no handler"
      (verify-empty-listener ItemListener :item-state-changed #(.itemStateChanged % nil)))
    (it "makes an instance that calls :item-state-changed"
      (verify-listener ItemListener :item-state-changed #(.itemStateChanged % nil))))
          
  (testing "for MouseListener"
    (it "instantiates an MouseListener instance"
      (instance? MouseListener (reify-listener MouseListener (ref {}))))
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
      (instance? MouseMotionListener (reify-listener MouseMotionListener (ref {}))))
    (it "makes an instance that does nothing when there's no handlers"
      (verify-empty-listener MouseMotionListener :mouse-moved #(.mouseMoved % nil)))
    (it "makes an instance that calls expected methods"
      (verify-listeners MouseMotionListener 
        :mouse-moved #(.mouseMoved % nil)
        :mouse-dragged #(.mouseDragged % nil)))))


(describe add-listener
  (it "can install a mouse-clicked listener"
    (let [panel        (JPanel.)
          f        (fn [e] (println "handled"))]
      (do
        (add-listener panel :mouse-clicked f)
        (expect (= 1 (-> panel .getMouseListeners count)))
        (expect (= f (-> (get-handlers panel :mouse) :mouse-clicked first))))))

  (it "can remove a mouse-clicked listener"
    (let [panel (JPanel.)
          f     (fn [e] (println "handled"))]
      (do
        (add-listener panel :mouse-clicked f)
        (remove-listener panel :mouse-clicked f)
        (expect (= 1 (-> panel .getMouseListeners count)))
        (expect (= 0 (-> (get-handlers panel :mouse) :mouse-clicked count))))))

  (it "can install two mouse-clicked listeners"
    (let [panel        (JPanel.)
          f        (fn [e] (println "handled"))
          g        (fn [e] (println "again!"))]
      (do
        (add-listener panel :mouse-clicked f :mouse-clicked g)
        (expect (= 1 (-> panel .getMouseListeners count)))
        (expect (= [g f] (-> panel (get-handlers :mouse) :mouse-clicked))))))

  (it "can install a document listener"
    (let [field        (JTextField.)
          called   (atom false)
          f        (fn [e] (reset! called true))]
      (do
        (add-listener field :insert-update f)
        (.setText field "force a change")
        (expect @called))))

  (it "can register for a class of events"
      (let [field    (JTextField.)
            called   (atom 0)
            f        (fn [e] (swap! called inc))]
        (do
          (add-listener field :document f)
          (.setText field "force insert")
          (.setText field ""))
        (expect (= 2 @called))))

  (it "can register for multiple events"
    (let [field    (JTextField.)
          called   (atom 0)
          f        (fn [e] (swap! called inc))]
      (do
        (add-listener field #{:remove-update :insert-update} f)
        (.setText field "force insert")
        (.setText field ""))
      (expect (= 2 @called))))
  (it "can register handlers on multiple targets"
    (let [button0  (JButton.)
          button1  (JButton.)
          called   (atom 0)
          f        (fn [e] (swap! called inc))]
      (do
        (add-listener [button0 button1] :action f)
        (.doClick button0)
        (.doClick button1))
      (expect (= 2 @called)))))

