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
  (:use [seesaw.meta :only [put-meta! get-meta]]
        [seesaw.util :only [camelize illegal-argument to-seq check-args]])
  (:import [javax.swing.event ChangeListener
            CaretListener DocumentListener
            ListSelectionListener
            TreeSelectionListener TreeExpansionListener TreeWillExpandListener TreeModelListener
            HyperlinkListener]
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
  javax.swing.JTabbedPane
  javax.swing.JViewport
  javax.swing.AbstractButton
  javax.swing.SingleSelectionModel
  javax.swing.SpinnerModel
  javax.swing.JSpinner
  javax.swing.ButtonModel)

(extend-listener-protocol AddActionListener add-action-listener addActionListener
  javax.swing.JFileChooser
  javax.swing.JTextField
  javax.swing.JComboBox
  javax.swing.AbstractButton
  javax.swing.ButtonModel
  javax.swing.ComboBoxEditor
  javax.swing.Timer
  java.awt.MenuItem)

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
  :tree-model {
    :name    :tree-model
    :class   TreeModelListener
    :events  #{:tree-nodes-changed :tree-nodes-inserted :tree-nodes-removed :tree-structure-changed}
    :install #(.addTreeModelListener ^javax.swing.tree.TreeModel %1 ^TreeModelListener %2)
  }

  :drag-source {
    :name         :drag-source
    :class        java.awt.dnd.DragSourceListener
    :events       #{:drag-drop-end :drag-enter :drag-exit :drag-over :drop-action-changed}
    ; Names are mostly the same as DragTarget events, so prefix with ds-
    ; See event-method-table below too!
    :named-events #{:ds-drag-drop-end :ds-drag-enter :ds-drag-exit :ds-drag-over :ds-drop-action-changed}
    :install      #(.addDragSourceListener ^java.awt.dnd.DragSource %1 ^java.awt.dnd.DragSourceListener %2)
  }

  :drag-source-motion {
    :name    :drag-source-motion
    :class   java.awt.dnd.DragSourceMotionListener
    :events  #{:drag-mouse-moved}
    :install #(.addDragSourceMotionListener ^java.awt.dnd.DragSource %1 ^java.awt.dnd.DragSourceMotionListener %2)
  }

  :drop-target {
    :name         :drop-target
    :class        java.awt.dnd.DropTargetListener
    :events       #{:drag-enter :drag-exit :drag-over :drop :drop-action-changed}
    ; Names are mostly the same as DragSource events, so prefix with dt-
    ; See event-method-table below too!
    :named-events #{:dt-drag-enter :dt-drag-exit :dt-drag-over :dt-drop :dt-drop-action-changed}
    :install      #(.addDropTargetListener ^java.awt.dnd.DropTarget %1 ^java.awt.dnd.DropTargetListener %2)
  }

  :hyperlink {
    :name    :hyperlink
    :class   HyperlinkListener
    :events  #{:hyperlink-update}
    :install #(.addHyperlinkListener ^javax.swing.JEditorPane %1
                                     ^HyperlinkListener %2)
  }
})

(def ^{:private true} event-groups-by-listener-class
  (into {}
        (for [{:keys [class] :as group} (vals event-groups)]
          [class group])))

(defn- get-listener-class [^java.lang.reflect.Method m]
  (let [[arg] (.getParameterTypes m)]
    (if (and arg (.startsWith (.getName m) "add"))
      arg)))


; Kind of a hack. Re-route methods with renamed events (due to collisions like
; valueChanged()) back to their real names.
(def ^{:private true} event-method-table (merge {
  :list-selection :value-changed
  :tree-selection :value-changed
 }
 (into {} (for [e (get-in event-groups [:drag-source :events])] [(keyword (str "ds-" (name e))) e]))
 (into {} (for [e (get-in event-groups [:drag-target :events])] [(keyword (str "dt-" (name e))) e]))))

(defmulti reify-listener (fn [& args] (first args)))

(defn- fire [hs ev-name e]
  (doseq [h (@hs ev-name)] (h e)))

(defmacro def-reify-listener
  [klass events]
  (let [hs (gensym "hs")]
    `(defmethod reify-listener ~klass [c# ~hs]
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
  (check-args (keyword? event-name) (str "Event name is not a keyword: " event-name))
  (let [event-group (event-group-table event-name)]
    (if-not event-group (illegal-argument "Unknown event type %s" event-name))
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
    ; Re-route to right listener type for :selection on various widget types
    (not= :selection event-name) event-name
    (instance? javax.swing.JList target)          :list-selection
    (instance? javax.swing.JTable target)         :list-selection
    (instance? javax.swing.JTree target)          :tree-selection
    (instance? javax.swing.JComboBox target)      :action-performed
    (instance? javax.swing.text.JTextComponent target) :caret-update
    (instance? java.awt.ItemSelectable target)    :item-state-changed
    (instance? javax.swing.JSpinner target)       :state-changed
    (instance? javax.swing.JSlider target)        :state-changed
    (instance? javax.swing.JTabbedPane target)    :state-changed
    :else event-name))

(defn- expand-multi-events
  "Expands an event spec into a seq of event names. This handles multi-event cases
  which can currently happen in two ways:

    * The caller provided a set of event names, e.g. #{:mouse-pressed :mouse-released}
    * The caller provided a 'composite' event name like :mouse
  "
  [target event-spec]
  (get-in event-groups [event-spec :events] (to-seq event-spec)))

(defn- preprocess-event-specs
  "take spec/fn pairs in listen arg list and expand multi-events, etc. For example:

    [:foo handler0 #{:bar :yum} handler1]

  becomes

    [:foo handler0 :bar handler1 :yum handler1]
  "
  [target args]
  (mapcat
    (fn [[a b]] (for [n (to-seq a)] [n b]))
    (for [[ens f] (partition 2 args)
          en (expand-multi-events target ens)]
      [en f])))

(defn- get-sub-targets
  "Expand targets into individual event sources. For example, a button-group is treated
  as the list of buttons it contains."
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

(defmulti listen-for-named-event
  "*experimental and subject to change*

  A multi-method that allows the set of events in the (listen) to be extended or
  for an existing event to be extended to a new type. Basically performs
  double-dispatch on the type of the target and the name of the event.

  This multi-method is an extension point, but is not meant to be called directly
  by client code.

  Register the given event handler on this for the given event
  name which is a keyword like :selection, etc. If the handler
  is registered, returns a zero-arg function that undoes the
  listener. Otherwise, must return nil indicating that no listener
  was registered, i.e. this doesn't support the given event.

  TODO try using this to implement all of the event system rather than the mess
  above.

  See:
    (seesaw.swingx/color-selection-button) for an example.
  "
  (fn [this event-name event-fn] [(type this) event-name]))

; Default impl just returns nil indicating no special handling for the event.
(defmethod listen-for-named-event :default [this event-name event-fn] nil)

(defn- single-target-listen-impl
  "Takes:

   * a single target
   * a raw-event-name (a keyword, set or keyword that eventually maps to a set
     of events, like :mouse)
   * an event handler function

  doall *must* be called to ensure all side-effects occur.

  Installs handlers in the target and returns a seq of functions which reverse
  this operation."
  [target raw-event-name event-fn]
  (check-args (or (var? event-fn) (fn? event-fn)) (str "Event handler for " raw-event-name " is not a function"))
  (doall
    (for [event-name (->> (expand-multi-events target raw-event-name)
                          (map #(resolve-event-aliases target %)))]
      (let [handlers          (get-or-install-handlers target event-name)
            final-method-name (get event-method-table event-name event-name)]
        (swap! handlers append-listener final-method-name event-fn)
        (fn []
          (swap! handlers unappend-listener final-method-name event-fn))))))

(defn- multi-target-listen-impl
  "Save as single-target-listen-impl, except that handlers are installed on multiple
  targets.

  Returns seq of functions that reverse the operation."

  ([targets raw-event-name event-fn]
    (apply concat
      (for [target targets]
        (if-let [hook-result (listen-for-named-event target raw-event-name event-fn)]
          [hook-result]
          (single-target-listen-impl target raw-event-name event-fn)))))

  ([targets raw-event-name event-fn & more]
   (concat (multi-target-listen-impl targets raw-event-name event-fn)
           (apply multi-target-listen-impl targets more))))

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
      #{:remove-update :insert-update} (fn [e] ...))

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
  (check-args (even? (count more))
              "List of event name/handler pairs must have even length")
  (let [all-targets (get-sub-targets (to-seq targets))
        remove-fns  (doall (apply multi-target-listen-impl all-targets more))]
    (apply juxt remove-fns)))

(defn listen-to-property
  "Listen to propertyChange events on a target for a particular named property.
  Like (listen), returns a function that, when called removes the installed
  listener."
  [^java.awt.Component target property event-fn]
  (let [listener (reify java.beans.PropertyChangeListener
                   (propertyChange [this e] (event-fn e)))]
    (.addPropertyChangeListener target property listener)
    (fn []
      (.removePropertyChangeListener target property listener))))

; :selection is an artificial event handled specially for each class
; of widget, so we hack...
(defn- selection-group-for [this]
  (if-let [group (event-group-table (resolve-event-aliases this :selection))]
    (-> group
      (assoc :name :selection))))

(defn events-for
  "Returns a sequence of event info maps for the given object which can
  be either a widget instance or class.

  Used by (seesaw.dev/show-events).

  See:
    (seesaw.dev/show-events)
  "
  [v]
  (let [base (->> (.getMethods (if (class? v) ^java.lang.Class v (class v)))
               (map get-listener-class)
               (filter identity)
               (map event-groups-by-listener-class)
               (filter identity)
               (map #(dissoc % :install)))
        selection (selection-group-for v)]
    (if selection
      (cons selection base)
      base)))
