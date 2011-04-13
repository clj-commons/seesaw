(ns seesaw.event
  (:use seesaw.util)
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

(defmacro def-reify-listener
  [klass events]
  (let [c (gensym) 
        hs (gensym)]
    `(defmethod ~'reify-listener ~klass [~c ~hs]
      (reify ~klass
        ~@(map (fn [event]
                (let [tx (gensym) 
                      ex (gensym) 
                      method (-> event name camelize symbol)]
                 `(~method [~tx ~ex] (fire ~hs ~event ~ex)))) events)))))
; ... makes something like this...
; (defmethod reify-listener ChangeListener [c hs]
;   (reify ChangeListener
;     (stateChanged [this e] (fire hs :state-changed e))))


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

;(defmacro def-reify-listeners
  ;[]
  ;(doseq [[_ event-group] event-groups]
    ;(let [c (:class event-group) evs (:events event-group)]
      ;`(def-reify-listener ~c ~evs))))
;(def-reify-listeners)

(def-reify-listener ChangeListener [:state-changed])
(def-reify-listener ItemListener [:item-state-changed])
(def-reify-listener MouseListener [:mouse-clicked :mouse-entered :mouse-exited :mouse-pressed :mouse-released])
(def-reify-listener MouseMotionListener [:mouse-moved :mouse-dragged])

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

