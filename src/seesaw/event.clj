(ns seesaw.event
  (:import [javax.swing.event ChangeListener]
           [java.awt.event ItemListener MouseListener MouseMotionListener]))

(defn append-listener
  [listeners k l]
  (update-in listeners [k] conj l))

; partial is important because (remove) doesn't take a coll as
; it's first argument!
(defn unappend-listener
  [listeners k l]
  (update-in listeners [k] (partial remove #{l})))

(defmulti reify-listener (fn [& args] (first args)))

(defn- fire [hs ev-name e]
  (doseq [h (@hs ev-name)] (h e)))

(defmethod reify-listener ChangeListener [c hs]
  (reify ChangeListener
    (stateChanged [this e] (fire hs :state-changed e))))

(defmethod reify-listener ItemListener [c hs]
  (reify ItemListener
    (itemStateChanged [this e] (fire hs :item-state-changed e))))

(defmethod reify-listener MouseListener [c hs]
  (reify MouseListener
    (mouseClicked  [this e] (fire hs :mouse-clicked e))
    (mouseEntered  [this e] (fire hs :mouse-entered e))
    (mouseExited   [this e] (fire hs :mouse-exited e))
    (mousePressed  [this e] (fire hs :mouse-pressed e))
    (mouseReleased [this e] (fire hs :mouse-released e))))

(defmethod reify-listener MouseMotionListener [c hs]
  (reify MouseMotionListener
    (mouseMoved   [this e] (fire hs :mouse-moved e))
    (mouseDragged [this e] (fire hs :mouse-dragged e))))

(def ^{:private true} event-groups {
  :change {
    :name    :change
    :class   ChangeListener
    :events  [:state-changed]
    :install #(.addChangeListener %1 %2)
  }
  :item {
    :name    :item
    :class   ItemListener
    :events  [:item-state-changed]
    :install #(.addItemListener %1 %2)
  }
  :mouse { 
    :name    :mouse
    :class   MouseListener
    :events  [:mouse-clicked :mouse-entered :mouse-exited :mouse-pressed :mouse-released]
    :install #(.addMouseListener %1 %2)
  }
  :mouse-motion { 
    :name    :mouse-motion
    :class   MouseMotionListener
    :events  [:mouse-moved :mouse-dragged]
    :install #(.addMouseMotionListener %1 %2)
  }
})

; (def-event-group MouseListener [:mouse-clicked ...] .addMouseListener)
; --> reify-listener MouseListener ...
;  add to group table
;  ???

; "reverse" the name mapping from event-groups above, e.g.
;   :mouse-entered -> :mouse struct
;   :mouse-clicked -> :mouse struct
;   ...
(def ^{:private true} event-group-table
  (apply hash-map (flatten (for [[k v] event-groups e (:events v)] [e v]))))

(defn- install-group-handlers
  [target event-group]
  (let [group-handlers (atom {})
        listener       (reify-listener (:class event-group) group-handlers)]
    (doto target
      ((:install event-group) listener)
      (.putClientProperty (:name event-group) group-handlers))
    group-handlers))

(defn- get-handlers*
  [target event-group-name]
  (.getClientProperty target event-group-name))

(defn get-handlers
  [target event-group-name]
  (when-let [hs (get-handlers* target event-group-name)]
    @hs))

(defn- get-or-install-handlers
  [target event-name]
  (let [event-group (event-group-table event-name)
        handlers    (get-handlers* target (:name event-group))]
    (if handlers
      handlers
      (install-group-handlers target event-group))))
      
(defn add-listener
  [target & more]
    (doseq [[event-name event-fn] (partition 2 more)]
      (let [handlers (get-or-install-handlers target event-name)]
        (swap! handlers append-listener event-name event-fn))))

; TODO remove-listener

(defn remove-listener
  [target & more]
  (doseq [[event-name event-fn] (partition 2 more)]
    (let [handlers (get-or-install-handlers target event-name)]
      (swap! handlers unappend-listener event-name event-fn))))

