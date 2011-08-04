;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for handling events. Do not use these functions directly.
            Use (seesaw.core/listen) instead."
      :author "Dave Ray"}
  seesaw.event
  (:use [seesaw util meta])
  (:import [javax.swing.event ChangeListener 
            CaretListener DocumentListener 
            ListSelectionListener 
            TreeSelectionListener TreeExpansionListener TreeWillExpandListener]
           [javax.swing.text Document]
           [java.awt.event WindowListener FocusListener ActionListener ItemListener 
                          MouseListener MouseMotionListener MouseWheelListener
                          KeyListener ComponentListener]
           [java.beans PropertyChangeListener]))

; Use some protocols for listener installation to avoid reflection

(defmacro ^{:private true } extend-listener-protocol [proto proto-method java-method & classes]
  `(extend-protocol ~proto
     ~@(mapcat (fn [c] `(~c (~proto-method [this# v#] (. this# ~java-method v#)))) classes)))

(defprotocol ^{:private true} AddChangeListener
  (add-change-listener [this l]))

(defprotocol ^{:private true} AddActionListener
  (add-action-listener [this v]))

(defprotocol ^{:private true} AddListSelectionListener
  (add-list-selection-listener [this v]))

(extend-listener-protocol AddChangeListener add-change-listener addChangeListener 
  javax.swing.BoundedRangeModel
  javax.swing.JProgressBar
  javax.swing.JSlider
  javax.swing.JSpinner
  javax.swing.JTabbedPane
  javax.swing.JViewport
  javax.swing.AbstractButton
  javax.swing.SingleSelectionModel
  javax.swing.SpinnerModel
  javax.swing.ButtonModel)
 
(extend-listener-protocol AddActionListener add-action-listener addActionListener 
  javax.swing.JFileChooser
  javax.swing.JTextField
  javax.swing.JComboBox
  javax.swing.AbstractButton
  javax.swing.ButtonModel
  javax.swing.ComboBoxEditor
  javax.swing.Timer)

(extend-listener-protocol AddListSelectionListener add-list-selection-listener addListSelectionListener
  javax.swing.JList
  javax.swing.ListSelectionModel)

(extend-protocol AddListSelectionListener
  javax.swing.JTable 
    (add-list-selection-listener [this l] 
      (add-list-selection-listener (.getSelectionModel this) l)))

; Declaratively set up all the Swing listener types available through the
; listen function below. The yucky fuctions and macros below take care
; of reifying the interface and mapping to clojure handler functions.
(def ^{:private true} event-groups {

  :component {
    :name    :component
    :class   ComponentListener
    :events  #{:component-hidden
               :component-moved
               :component-resized
               :component-shown}
    :install #(.addComponentListener ^java.awt.Component %1 ^ComponentListener %2)
  }
                                    
  :property-change {
    :name    :property-change
    :class   PropertyChangeListener
    :events  #{:property-change}
    :install #(.addPropertyChangeListener ^java.awt.Component %1 ^PropertyChangeListener %2)
  }
  :key {
    :name    :key
    :class   KeyListener
    :events  #{:key-pressed :key-released :key-typed}
    :install #(.addKeyListener ^java.awt.Component %1 ^KeyListener %2)
  }
  :window {
    :name    :window
    :class   WindowListener
    :events  #{:window-activated :window-deactivated 
              :window-closed :window-closing :window-opened
              :window-deiconified :window-iconified}
    :install  #(.addWindowListener ^java.awt.Window %1 ^WindowListener %2)
  }
  :focus {
    :name    :focus
    :class   FocusListener
    :events  #{:focus-gained :focus-lost}
    :install #(.addFocusListener ^java.awt.Component %1 ^FocusListener %2)
  }
  :document {
    :name    :document
    :class   DocumentListener
    :events  #{:changed-update :insert-update :remove-update}
    :install (fn [target listener] 
               (.addDocumentListener 
                 (if (instance? Document target) 
                    ^Document target
                    (.getDocument ^javax.swing.text.JTextComponent target))
                 ^DocumentListener listener))
  }
  :caret {
    :name    :caret
    :class   CaretListener 
    :events  #{:caret-update}
    :install #(.addCaretListener ^javax.swing.text.JTextComponent %1 ^CaretListener %2)
  }
  :action {
    :name    :action
    :class   ActionListener
    :events  #{:action-performed}
    :install add-action-listener
  }
  :change {
    :name    :change
    :class   ChangeListener
    :events  #{:state-changed}
    :install add-change-listener
  }
  :item {
    :name    :item
    :class   ItemListener
    :events  #{:item-state-changed}
    :install #(.addItemListener ^java.awt.ItemSelectable %1 ^ItemListener %2)
  }
  :mouse { 
    :name    :mouse
    :class   MouseListener
    :events  #{:mouse-clicked :mouse-entered :mouse-exited :mouse-pressed :mouse-released}
    :install #(.addMouseListener ^java.awt.Component %1 ^MouseListener %2)
  }
  :mouse-motion { 
    :name    :mouse-motion
    :class   MouseMotionListener
    :events  #{:mouse-moved :mouse-dragged}
    :install #(.addMouseMotionListener ^java.awt.Component %1 ^MouseMotionListener %2)
  }
  :mouse-wheel { 
    :name    :mouse-wheel
    :class   MouseWheelListener
    :events  #{:mouse-wheel-moved}
    :install #(.addMouseWheelListener ^java.awt.Component %1 ^MouseWheelListener %2)
  }
  :list-selection { 
    :name    :list-selection
    :class   ListSelectionListener
    :events  #{:value-changed}
    :named-events #{:list-selection} ; Suppress reversed map entry
    :install add-list-selection-listener
  }
  :tree-selection { 
    :name    :tree-selection
    :class   TreeSelectionListener
    :events  #{:value-changed}
    :named-events #{:tree-selection} ; Suppress reversed map entry
    :install #(.addTreeSelectionListener ^javax.swing.JTree %1 ^TreeSelectionListener %2)
  }
  :tree-expansion { 
    :name    :tree-expansion
    :class   TreeExpansionListener 
    :events  #{:tree-expanded :tree-collapsed}
    :install #(.addTreeExpansionListener ^javax.swing.JTree %1 ^TreeExpansionListener %2)
  }
  ; Since one of the methods matches the listener name, we give the overall
  ; a slightly different name to distinguish registering for *all* events
  ; versus just one.
  :tree-will-expand* { 
    :name    :tree-will-expand*
    :class   TreeWillExpandListener
    :events  #{:tree-will-expand :tree-will-collapse}
    :install #(.addTreeWillExpandListener ^javax.swing.JTree %1 ^TreeWillExpandListener %2)
  }
})

; Kind of a hack. Re-route methods with renamed events (due to collisions like
; valueChanged()) back to their real names.
(def ^{:private true} event-method-table {
  :list-selection :value-changed
  :tree-selection :value-changed
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
    ~@(for [[_ {^Class klass :class events :events}] event-groups]
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
  (->>
    (for [[k v] event-groups e (or (:named-events v) (:events v))] [e v])
    (flatten)
    (apply hash-map)))

(defn- store-handlers
  [target event-group-name handlers]
  (put-meta! target event-group-name handlers))

(defn- install-group-handlers
  [target event-group]
  (let [group-handlers (atom {})
        listener       (reify-listener (:class event-group) group-handlers)]
    (doto target
      ((:install event-group) listener)
      (store-handlers (:name event-group) group-handlers))
    group-handlers))

(defn- get-handlers*
  [target event-group-name]
  (get-meta target event-group-name))

(defn get-handlers
  [target event-group-name]
  (when-let [hs (get-handlers* target event-group-name)]
    @hs))

(defn- get-or-install-handlers
  [target event-name]
  (let [event-group (event-group-table event-name)]
    (if-not event-group (throw (IllegalArgumentException. (str "Unknown event type " event-name))))
    (if-let [handlers (get-handlers* target (:name event-group))]
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

(defn- resolve-event-aliases
  [target event-name]
  (cond
    (not= :selection event-name) (get-in event-groups [event-name :events] event-name)
    ; Re-route to right listener type for :selection on various widget types
    (instance? javax.swing.JList target)          :list-selection
    (instance? javax.swing.JTable target)         :list-selection
    (instance? javax.swing.JTree target)          :tree-selection
    (instance? javax.swing.JComboBox target)      :action-performed
    (instance? javax.swing.text.JTextComponent target) :caret-update
    (instance? java.awt.ItemSelectable target)    :item-state-changed
    :else event-name))

(defn- preprocess-event-specs
  "take name/fn pairs in listen arg list and resolve aliases
   and stuff"
  [target args]
  (mapcat 
    (fn [[a b]] (for [n (to-seq a)] [n b]))
    (for [[en f] (partition 2 args)]
      [(resolve-event-aliases target en) f])))

(defn- remove-listener
  "Remove one or more listener function from target which were
   previously added with (listen)"
  [targets & more]
  (doseq [target (to-seq targets)
          [event-name event-fn] (preprocess-event-specs target more)]
    ; TODO no need to install handlers if they're not already there.
    (let [handlers (get-or-install-handlers target event-name)
          final-method-name (get event-method-table event-name event-name)]
      (swap! handlers unappend-listener final-method-name event-fn)))
    targets)

(defn- get-sub-targets
  [targets]
  (reduce
    (fn [result target]
      (cond
        (instance? javax.swing.ButtonGroup target) 
          (concat result (enumeration-seq (.getElements ^javax.swing.ButtonGroup target)))
        :else 
          (conj result target)))  
    []
    targets))
    
(defn listen
  "
  *note: use seesaw.core/listen rather than calling this directly*

  Install listeners for one or more events on the given target. For example:

    (listen (button \"foo\")
      :mouse-entered     (fn [e] ...)
      :focus-gained      (fn [e] ...)
      :key-pressed       (fn [e] ...)
      :mouse-wheel-moved (fn [e] ...))

  one function can be registered for multiple events by using a set 
  of event names instead of one:

    (listen (text)
      #{:remove-update insert-update} (fn [e] ...))

  Note in this case that it's smart enough to add a document listener
  to the JTextFields document.

  Similarly, an event can be registered for all events in a particular swing
  listener interface by just using the keyword-ized prefix of the interface
  name. For example, to get all callbacks in the MouseListener interface:

    (listen my-widget :mouse (fn [e] ...))

  Returns a function which, when called, removes all listeners registered
  with this call.

  When the target is a JTable and listener type is :selection, only
  row selection events are reported. Also note that the source table is
  *not* retrievable from the event object.
  "
  [targets & more]
  (check-args (even? (count more)) "List of event name/handler pairs must have even length")
  (doseq [target (get-sub-targets (to-seq targets))
          [event-name event-fn] (preprocess-event-specs target more)]
    (check-args (keyword? event-name) (str "Event name is not a keyword: " event-name))
    (check-args (fn? event-fn) (str "Event handler for " event-name " is not a function"))
    (let [handlers (get-or-install-handlers target event-name)
          final-method-name (get event-method-table event-name event-name)]
      (swap! handlers append-listener final-method-name event-fn)))
  (fn [] (apply remove-listener targets more)))


