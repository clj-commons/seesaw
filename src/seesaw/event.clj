(ns seesaw.event
  (:import [javax.swing.event ChangeListener]
           [java.awt.event ItemListener MouseListener MouseMotionListener]))

(defn add-listener
  [listeners k l]
  (update-in listeners [k] conj l))

; partial is important because (remove) doesn't take a coll as
; it's first argument!
(defn remove-listener
  [listeners k l]
  (update-in listeners [k] (partial remove #{l})))

(defmulti make-listener (fn [& args] (first args)))

(defn- fire [hs ev-key e]
  (doseq [h (@hs ev-key)] (h e)))

(defmethod make-listener ChangeListener [c hs]
  (reify ChangeListener
    (stateChanged [this e] (fire hs :state-changed e))))

(defmethod make-listener ItemListener [c hs]
  (reify ItemListener
    (itemStateChanged [this e] (fire hs :item-state-changed e))))

(defmethod make-listener MouseListener [c hs]
  (reify MouseListener
    (mouseClicked  [this e] (fire hs :mouse-clicked e))
    (mouseEntered  [this e] (fire hs :mouse-entered e))
    (mouseExited   [this e] (fire hs :mouse-exited e))
    (mousePressed  [this e] (fire hs :mouse-pressed e))
    (mouseReleased [this e] (fire hs :mouse-released e))))

(defmethod make-listener MouseMotionListener [c hs]
  (reify MouseMotionListener
    (mouseMoved   [this e] (fire hs :mouse-moved e))
    (mouseDragged [this e] (fire hs :mouse-dragged e))))
