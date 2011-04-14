(ns seesaw.event
  (:use seesaw.util)
  (:import [javax.swing.event ChangeListener DocumentListener]
           [java.awt.event WindowListener FocusListener ActionListener ItemListener 
                          MouseListener MouseMotionListener MouseWheelListener
                          KeyListener]
           [java.beans PropertyChangeListener]))

; Declaratively set up all the Swing listener types available through the
; add-listener function below. The yucky fuctions and macros below take care
; of reifying the interface and mapping to clojure handler functions.
(def ^{:private true} event-groups {

  :property-change {
    :name    :property-change
    :class   PropertyChangeListener
    :events  [:property-change]
    :install #(.addPropertyChangeListener %1 %2)
  }
  :key {
    :name    :key
    :class   KeyListener
    :events  [:key-pressed :key-released :key-typed]
    :install #(.addKeyListener %1 %2)
  }
  :window {
    :name    :window
    :class   WindowListener
    :events  [:window-activated :window-deactivated 
              :window-closed :window-closing :window-opened
              :window-deiconified :window-iconified]
    :install #(.addWindowListener %1 %2)
  }
  :focus {
    :name    :focus
    :class   FocusListener
    :events  [:focus-gained :focus-lost]
    :install #(.addFocusListener %1 %2)
  }
  :document {
    :name    :document
    :class   DocumentListener
    :events  [:changed-update :insert-update :remove-update]
    :install #(.addDocumentListener %1 %2)
  }
  :action {
    :name    :action
    :class   ActionListener
    :events  [:action-performed]
    :install #(.addActionListener %1 %2)
  }
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
  :mouse-wheel { 
    :name    :mouse-wheel
    :class   MouseWheelListener
    :events  [:mouse-wheel-moved]
    :install #(.addMouseWheelListener %1 %2)
  }
})

(defmulti reify-listener (fn [& args] (first args)))

(defn- fire [hs ev-name e]
  (doseq [h (@hs ev-name)] (h e)))

(defmacro def-reify-listener
  [klass events]
  (let [hs (gensym "hs")]
    `(defmethod ~'reify-listener ~klass [c# ~hs]
      (reify ~klass
        ~@(for [event events] 
          `(~(-> event name camelize symbol) [tx# ex#] (fire ~hs ~event ex#)))))))

; ... makes something like this ...
; (defmethod reify-listener ChangeListener [c hs]
;   (reify ChangeListener
;     (stateChanged [this e] (fire hs :state-changed e))))


; Make a macro to reify all the listener classes/methods above and call
; it. 
(defmacro ^{:private true} reify-all-event-groups
  []
  `(do
    ~@(for [[_ {klass :class events :events}] event-groups]
      ; the symbol is very important here since the def-reify-listener
      ; macro is expecting a symbol NOT a class instance! So many hours 
      ; wasted...
      `(def-reify-listener ~(symbol (.getName klass)) ~events))))

(reify-all-event-groups)

; ... is equivalent to a bunch of macro expansions like this ...
;(def-reify-listener ChangeListener [:state-changed])
; ... one for each entry in the event-groups map above.


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

(defn append-listener
  [listeners k l]
  (update-in listeners [k] conj l))

; partial is important because (remove) doesn't take a coll as
; it's first argument!
(defn unappend-listener
  [listeners k l]
  (update-in listeners [k] (partial remove #{l})))

      
(defn add-listener
  "Install listeners for one or more events on the given target. For example:

    (add-listener (button \"foo\")
      :mouse-entered     (fn [e] ...)
      :focus-gained      (fn [e] ...)
      :key-pressed       (fn [e] ...)
      :mouse-wheel-moved (fn [e] ...))

  Functions can be removed with (remove-listener).
  "
  [target & more]
    (doseq [[event-name event-fn] (partition 2 more)]
      (let [handlers (get-or-install-handlers target event-name)]
        (swap! handlers append-listener event-name event-fn))))

(defn remove-listener
  "Remove one or more listener function from target which were
   previously added with (add-listener)"
  [target & more]
  (doseq [[event-name event-fn] (partition 2 more)]
    (let [handlers (get-or-install-handlers target event-name)]
      (swap! handlers unappend-listener event-name event-fn))))

