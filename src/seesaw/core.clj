;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.core
  (:use [seesaw util font border color meta]
        [clojure.string :only (capitalize split)])
  (:require [seesaw.invoke]
            [seesaw.event :as sse]
            [seesaw.timer :as sst]
            [seesaw.selection :as sss]
            [seesaw.icon :as ssi]
            [seesaw.action :as ssa]
            [seesaw.table :as ss-table]
            [seesaw.cells :as cells]
            [seesaw.bind :as ssb]
            [seesaw.graphics :as ssg])
  (:import [java.util EventObject]
           [javax.swing 
             SwingUtilities SwingConstants UIManager ScrollPaneConstants
             Action
             BoxLayout
             JDialog JFrame JComponent Box JPanel JScrollPane JSplitPane JToolBar JTabbedPane
             JLabel JTextField JTextArea 
             AbstractButton JButton JToggleButton JCheckBox JRadioButton ButtonGroup
             JOptionPane]
           [javax.swing.text JTextComponent]
           [java.awt Component FlowLayout BorderLayout GridLayout 
              GridBagLayout GridBagConstraints
              Dimension]))

(declare to-widget)
(declare popup-option-handler)

(def #^{:macro true :doc "Alias for seesaw.invoke/invoke-now"} invoke-now #'seesaw.invoke/invoke-now)
(def #^{:macro true :doc "Alias for seesaw.invoke/invoke-later"} invoke-later #'seesaw.invoke/invoke-later)

(defn native!
  "Set native look and feel and other options to try to make things look right.
  This function must be called very early, like before any other Seesaw or Swing
  calls!
  
  Note that on OSX, you can set the application name in the menu bar (usually
  displayed as the main class name) by setting the -Xdock:<name-of-your-app>
  parameter to the JVM at startup. Sorry, I don't know of a way to do it 
  dynamically.

  See:

  http://developer.apple.com/library/mac/#documentation/Java/Conceptual/Java14Development/07-NativePlatformIntegration/NativePlatformIntegration.html
  "
  []
  (System/setProperty "apple.laf.useScreenMenuBar" "true")
  (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName)))

(defn assert-ui-thread
  "Verify that the current thread is the Swing UI thread and throw
  IllegalStateException if it's not. message is included in the exception
  message.

  Returns nil.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/SwingUtilities.html#isEventDispatchThread%28%29
  "
  [message]
  (when-not (SwingUtilities/isEventDispatchThread)
    (throw (IllegalStateException. 
             (str "Expected UI thread, but got '"
                  (.. (Thread/currentThread) getName)
                  "' : "
                  message)))))

; TODO make a macro for this. There's one in contrib I think, but I don't trust contrib.

; alias timer/timer for convenience
(def ^{:doc (str "Alias of seesaw.timer/timer:\n" (:doc (meta #'sst/timer)))} timer sst/timer)

; alias event/listen for convenience
(def ^{:doc (str "Alias of seesaw.event/listen:\n" (:doc (meta #'sse/listen)))} listen sse/listen)

; alias action/action for convenience
(def ^{:doc (str "Alias of seesaw.action/action:\n" (:doc (meta #'ssa/action)))} action ssa/action)


; TODO protocol or whatever when needed
(defn- to-selectable
  [target]
  (cond
    (instance? javax.swing.ButtonGroup target) target
    :else (to-widget target)))

(defn selection 
  "Gets the selection of a widget. target is passed through (to-widget)
  so event objects can also be used. The default behavior is to return
  a *single* selection value, even if the widget supports multiple selection.
  If there is no selection, returns nil.

  options is an option map which supports the following flags:

    multi? - If true the return value is a seq of selected values rather than
      a single value.

  Examples:

  (def t (table))
  (listen t :selection 
    (fn [e]
      (let [selected-rows (selection t {:multi? true})]
        (println \"Currently selected rows: \" selected-rows))))
  
  See:

    seesaw.selection/selection.
  "
  ([target] (selection target {}))
  ([target options] (sss/selection (to-selectable target) options)))

(defn selection!
  "Sets the selection on a widget. target is passed through (to-widget)
  so event objects can also be used. The arguments are the same as
  (selection). By default, new-selection is a single new selection value.
  If new-selection is nil, the selection is cleared.

  options is an option map which supports the following flags:

    multi? - if true new-expression is a list of values to selection,
      the same as the list returned by (selection).

  Always returns target.

  See also seesaw.selection/selection!.
  "
  ([target new-selection] (selection! target {} new-selection))
  ([target opts new-selection] (sss/selection! (to-selectable target) opts new-selection)))

(def icon ssi/icon)
(def ^{:private true} make-icon icon)

;*******************************************************************************
; Widget coercion prototcol

(defprotocol ToWidget (to-widget* [v create?]))

; A couple macros to make definining the ToWidget protocol a little less
; tedious. Mostly just for fun...

(defmacro ^{:private true} def-widget-coercion [t b & forms]
  `(extend-type 
     ~t
     ToWidget 
     (~'to-widget* [~(first b) create?#] ~@forms)))

(defmacro ^{:private true} def-widget-creational-coercion [t b & forms]
  `(extend-type 
     ~t
     ToWidget 
     (~'to-widget* [~(first b) create?#] (when create?# ~@forms))))

; ... for example, a component coerces to itself.
(def-widget-coercion java.awt.Component [c] c)

(def-widget-coercion java.util.EventObject 
  [v] 
  (try-cast java.awt.Component (.getSource v)))

(def-widget-creational-coercion java.awt.Dimension [v] (Box/createRigidArea v))

(def-widget-creational-coercion javax.swing.Action [v] (JButton. v))

(def-widget-creational-coercion clojure.lang.Keyword 
  [v] 
  (condp = v
    :separator (javax.swing.JSeparator.)
    :fill-h (Box/createHorizontalGlue)
    :fill-v (Box/createVerticalGlue)))

(def-widget-creational-coercion clojure.lang.IPersistentVector 
  [[v0 v1 v2]]
  (cond
    (= :fill-h v0) (Box/createHorizontalStrut v1)
    (= :fill-v v0) (Box/createVerticalStrut v1)
    (= :by v1) (Box/createRigidArea (Dimension. v0 v2))))

(def-widget-creational-coercion Object
  [v]
  (JLabel. (str v)))

(def-widget-creational-coercion java.net.URL
  [v]
  (JLabel. (make-icon v)))

(defn to-widget 
  "Try to convert the input argument to a widget based on the following rules:

    nil -> nil
    java.awt.Component -> return argument unchanged
    java.awt.Dimension -> return Box/createRigidArea
    java.swing.Action    -> return a button using the action
    java.util.EventObject -> return the event source
    :separator -> create a horizontal JSeparator
    :fill-h -> Box/createHorizontalGlue
    :fill-v -> Box/createVerticalGlue
    [:fill-h n] -> Box/createHorizontalStrut with width n
    [:fill-v n] -> Box/createVerticalStrut with height n
    [width :by height] -> create rigid area with given dimensions
    A java.net.URL -> a label with the image located at the url
    Anything else -> a label with the text from passing the object through str

   If create? is false, will return nil for all rules (see above) that
   would create a new widget. The default value for create? is false
   to avoid inadvertently creating widgets all over the place.
  "
  ([v]         (to-widget v false))
  ([v create?] (when v (to-widget* v create?))))

;*******************************************************************************
; Widget construction stuff

(def ^{:private true :dynamic true} *with-widget* nil)

(defmacro with-widget
  "This macro allows a Seesaw widget 'constructor' function to be applied to
  a sub-class of the widget type it usually produces. For example (listbox)
  always returns an instance of exactly JList. Suppose you're using SwingX
  and want to use the Seesaw goodness of (listbox), but want to get a 
  JXList. That's what this macro is for:

    (with-widget org.jdesktop.swingx.JXList
      (listbox :id :my-list :model ...))

  This will return a new instance of JXList, with the usual Seesaw listbox
  options applied.

  The factory argument can be one of the following:

    A class literal - .newInstance is used to create a new instance of
                      the class.

    A function - The function is called with no arguments. It should
                 return a sub-class of the expected class.

    An existing instance - The instance is modified and returned.

  If the instance in any of these cases is not a sub-class of the
  type usually created by the constructor function, an IllegalArgumentException
  is thrown. For example:

    (with-widget JLabel (listbox ...)) ==> IllegalArgumentException

  Returns a fully initialized instance of the class created by the
  provided factory.
  "
  [factory form]
  `(binding [*with-widget* ~factory]
     ~form))

(defn- construct 
  "Use the current *with-widget* binding to create a new widget, ensuring the
   result is consistent with the given expected class. If there's no 
   *with-widget* binding, just fallback to a default instance of the expected
   class.
  
  Returns an instance of the expected class, or throws IllegalArgumentException
  if the result using *with-widget* isn't consistent with expected-class."
  ([factory-class] (construct (or *with-widget* factory-class) factory-class))
  ([factory expected-class]
    (cond
      (instance? expected-class factory) 
        factory

      (class? factory) 
        (construct #(.newInstance factory) expected-class)

      (fn? factory)
        (let [result (factory)]
          (if (instance? expected-class result)
            result
            (throw (IllegalArgumentException. 
                     (str (class result) " is not an instance of " expected-class)))))

      :else 
        (throw (IllegalArgumentException. 
                 (str "Factory or instance " factory 
                      " is not consistent with expected type " expected-class))))))

;*******************************************************************************
; Generic widget stuff

(declare show-modal-dialog)
(declare to-root)
(declare is-modal-dialog?)

(defprotocol Showable
  (visible! [this v])
  (visible? [this]))

(extend-protocol Showable
  java.awt.Component
    (visible! [this v] (doto this (.setVisible (boolean v))))
    (visible? [this] (.isVisible this))
  java.awt.Dialog
    (visible! [this v]
      (if (and v (is-modal-dialog? this))
        (show-modal-dialog this)
        (doto this (.setVisible false))))
  java.util.EventObject
    (visible! [this v] (visible! (.getSource this) v))
    (visible? [this] (visible? (.getSource this))))

(defn show!
  "Show a frame, dialog or widget.
   
   If target is a modal dialog, the call will block and show! will return the
   dialog's result. See (seesaw.core/return-from-dialog).

   Returns its input.

  See:
    http://download.oracle.com/javase/6/docs/api/java/awt/Window.html#setVisible%28boolean%29
  "
  [targets]
  (if (is-modal-dialog? targets)
    (visible! targets true)
    (do
      (doseq [target (to-seq targets)]
        (visible! target true))
      targets)))

(defn hide!
  "Hide a frame, dialog or widget.
   
   Returns its input.

  See:
    http://download.oracle.com/javase/6/docs/api/java/awt/Window.html#setVisible%28boolean%29
  "
  [targets]
  (doseq [target (to-seq targets)]
    (visible! target false))
  targets)

(defn pack!
  "Pack a frame or window, causing it to resize to accommodate the preferred
  size of its contents.

  Returns its input.

  See:
    http://download.oracle.com/javase/6/docs/api/java/awt/Window.html#pack%28%29 
  "
  [targets]
  (doseq [#^java.awt.Window target (map to-root (to-seq targets))]
    (.pack target))
  targets)

(defn dispose!
  "Dispose the given frame, dialog or window. target can be anything that can
  be converted to a root-level object with (to-root).

  Returns its input. 

  See:
   http://download.oracle.com/javase/6/docs/api/java/awt/Window.html#dispose%28%29 
  "
  [targets]
  (doseq [#^java.awt.Window target (map to-root (to-seq targets))]
    (.dispose target))
  targets)

(defn repaint!
  "Request a repaint of one or a list of widget-able things.

  Example:

    ; Repaint just one widget
    (repaint! my-widget)

    ; Repaint all widgets in a hierarcy
    (repaint! (select [:*] root))

  Returns targets.
  "
  [targets]
  (doseq [target (map to-widget (to-seq targets))]
    (.repaint target))
  targets)

(defn- handle-structure-change [container]
  "Helper. Revalidate and repaint a container after structure change"
  (doto container
    .revalidate
    .repaint))

;*******************************************************************************
; move!

(defprotocol ^{:private true} Movable
  (move-to! [this x y])
  (move-by! [this dx dy])
  (move-to-front! [this])
  (move-to-back! [this]))

; A protocol impl can't have a partial implementation, so these are
; here for re-use.
(defn- move-component-to! [^java.awt.Component this x y]
  (let [old-loc (.getLocation this)
        x (or x (.x old-loc))
        y (or y (.y old-loc))]
    (doto this (.setLocation x y))))

(defn- move-component-by! [^java.awt.Component this dx dy]
  (let [old-loc (.getLocation this)
        x (.x old-loc)
        y (.y old-loc)]
    (doto this (.setLocation (+ x dx) (+ y dy)))))

(extend-protocol Movable
  java.util.EventObject
    (move-to! [this x y]   (move-to! (.getSource this) x y))
    (move-by! [this dx dy] (move-by! (.getSource this) dx dy))
    (move-to-front! [this] (move-to-front! (.getSource this)))
    (move-to-back! [this]  (move-to-back! (.getSource this)))
  java.awt.Component
    (move-to! [this x y]   (move-component-to! this x y))
    (move-by! [this dx dy] (move-component-by! this dx dy))
    (move-to-front! [this] 
      (do
        (doto (.getParent this)
          (.setComponentZOrder this 0)
          handle-structure-change)
        this))
    (move-to-back! [this]
      (let [parent (.getParent this)
            n      (.getComponentCount parent)]
        (doto parent 
          (.setComponentZOrder this (dec n))
          handle-structure-change)
        this))
  java.awt.Window
    (move-to! [this x y]   (move-component-to! this x y))
    (move-by! [this dx dy] (move-component-by! this dx dy))
    (move-to-front! [this] (doto this .toFront))
    (move-to-back! [this] (doto  this .toBack)))
    
(defn move!
  "Move a widget relatively or absolutely. target is a 'to-widget'-able object,
  type is :by or :to, and loc is a two-element vector or instance of 
  java.awt.Point. The how type parameter has the following interpretation:

    :to The absolute position of the widget is set to the given point
    :by The position of th widget is adjusted by the amount in the given point
        relative to its current position.
    :to-front Move the widget to the top of the z-order in its parent.

  Returns target.

  Examples:

    ; Move x to the point (42, 43)
    (move! x :to [42, 43])

    ; Move x to y position 43 while keeping x unchanged
    (move! x :to [:*, 43])

    ; Move x relative to its current position. Assume initial position is (42, 43).
    (move! x :by [50, -20])
    ; ... now x's position is [92, 23]

  Notes: 
    For widgets, this function will generally only have an affect on widgets whose container
    has a nil layout! This function has similar functionality to the :bounds
    and :location options, but is a little more flexible and readable.

  See:
    (seesaw.core/xyz-panel)
    http://download.oracle.com/javase/6/docs/api/java/awt/Component.html#setLocation(int, int)
  "
  [target how & [loc]]
  (check-args (#{:by :to :to-front :to-back} how) "Expected :by, :to, :to-front, :to-back in move!")
  (case how
    (:to :by)
      (let [[x y] (cond 
                    (instance? java.awt.Point loc) [(.x loc) (.y loc)] 
                    (instance? java.awt.Rectangle loc) [(.x loc) (.y loc)] 
                    (= how :to) (replace {:* nil} loc)
                    :else loc)]
        (case how
          :to      (move-to! target x y)
          :by      (move-by! target x y)))
    :to-front
      (move-to-front! target)
    :to-back
      (move-to-back! target)))


(defn- add-widget 
  ([c w] (add-widget c w nil))
  ([c w constraint] 
    (let [w* (to-widget w true)]
      (check-args (not (nil? w*)) (str "Can't add nil widget. Original was (" w ")"))
      (.add c w* constraint)
      w*)))

(defn- add-widgets
  [c ws]
  (.removeAll c)
  (doseq [w ws]
    (add-widget c w))
  (handle-structure-change c))

(def ^{:private true} id-property ::seesaw-widget-id)

(defn id-for 
  "Returns the id of the given widget if the :id property was specified at
   creation. The widget parameter is passed through (to-widget) first so
   events and other objects can also be used. The id is always returned as
   a string, even it if was originally given as a keyword.

  Returns the id as a string, or nil.
  
  See:
    (seesaw.core/select).
  "
  [w] 
  (get-meta (to-widget w) id-property))

(defn- id-option-handler [w id]
  (let [id-key (name id)
        existing-id (get-meta w id-property)]
    (when existing-id (throw (IllegalStateException. (str ":id is already set to " existing-id))))
    ; TODO should we enforce unique ids?
    (put-meta! w id-property id-key)))

(def ^{:private true} h-alignment-table 
  (constant-map SwingConstants :left :right :leading :trailing :center ))

(def ^{:private true} v-alignment-table
  (constant-map SwingConstants :top :center :bottom))

(def ^{:private true} orientation-table
  (constant-map SwingConstants :horizontal :vertical))

(defn- bounds-option-handler [^java.awt.Component target v]
  (cond
    ; TODO to-rect protocol?
    (= :preferred v)
      (bounds-option-handler target (.getPreferredSize target))
    (instance? java.awt.Rectangle v) (.setBounds target v)
    (instance? java.awt.Dimension v) 
      (let [loc (.getLocation target)]
        (.setBounds target (.x loc) (.y loc) (.width v) (.height v)))
    :else
      (let [old       (.getBounds target)
            [x y w h] (replace {:* nil} v)]
        (.setBounds target 
            (or x (.x old))     (or y (.y old)) 
            (or w (.width old)) (or h (.height old))))))


;*******************************************************************************
; Widget configuration stuff
(defprotocol ConfigureWidget (config* [target args]))

(defn config!
  "Applies properties in the argument list to one or more targets. For example:

    (config! button1 :enabled? false :text \"I' disabled\")

  or:

    (config! [button1 button2] :enabled? false :text \"We're disabled\")
 
  Targets may be actual widgets, or convertible to widgets with (to-widget).
  For example, the target can be an event object.

  Returns the input targets."
  [targets & args]
  (doseq [target (to-seq targets)]
    (config* target args))
  targets)


;*******************************************************************************
; Property<->Atom syncing

(def ^{:private true} short-property-keywords-to-long-map
     {:min :minimum
      :max :maximum
      :tip :tool-tip-text})

(defn- kw->java-name
  "(kw->java-name :preferred-size)"
  [kw]
  (reduce str
          (map capitalize (split (-> (name kw)
                                     (.replace "?" ""))
                                 #"\-"))))

(defn property-kw->java-name
  "INTERNAL USE ONLY. DO NOT USE
  (property-kw->java-name :tip)"
  [kw]
  (apply str
          (map capitalize (split (-> (short-property-keywords-to-long-map kw kw)
                                     name
                                     (.replace "?" ""))
                                 #"\-"))))

(defn- kw->java-method
  "USED ONLY BY TESTS. DO NOT USE.
  (kw->java-method :enabled?)"
  [kw]
  (str (if (.endsWith (str kw) "?")
         "is"
         "get") (kw->java-name kw)))

(defn property-kw->java-method
  "USED ONLY BY TESTS. DO NOT USE.
  (property-kw->java-method :tip)"
  [kw]
  (kw->java-method (get short-property-keywords-to-long-map kw kw)))

;; by default, property names' first character will be lowercased when
;; added using a property change listener. For some however, the first
;; character must stay uppercased. This map will specify those exceptions.
(def ^{:private true} property-change-listener-name-overrides {
  "ToolTipText" "ToolTipText"
})

(defmulti ^{:private true} setup-property-change-on-atom (fn [c k a] [(type c) k]))

(defmethod ^{:private true} setup-property-change-on-atom :default
  [component property a]
  (let [property-name (property-kw->java-name property)]
    (.addPropertyChangeListener
     component
     ; first letter of *some* property-names must be lower-case
     (property-change-listener-name-overrides
        property-name
        (apply str (clojure.string/lower-case (first property-name)) (rest property-name)))
     (proxy [java.beans.PropertyChangeListener] [] 
       (propertyChange [e] (reset! a (.getNewValue e)))))))

(defn- setup-property-syncing
  [component property-name a]
  (add-watch a
             (keyword (gensym "property-syncing-watcher"))
             (fn atom-watcher-fn
               [k r o n] (when-not (= o n)
                           (invoke-now (config! component
                                                property-name
                                                n)))))
  (setup-property-change-on-atom component property-name a))

(defn- ensure-sync-when-atom
  [component property-name atom-or-other]
  (if (atom? atom-or-other)
    (do (setup-property-syncing component property-name atom-or-other) @atom-or-other)
    atom-or-other))


;*******************************************************************************
; Default options
(def ^{:private true} default-options {
  :id          id-option-handler
  :listen      #(apply sse/listen %1 %2)
  :opaque?     #(.setOpaque %1 (boolean (ensure-sync-when-atom %1 :opaque? %2)))
  :enabled?    #(.setEnabled %1 (boolean (ensure-sync-when-atom %1 :enabled? %2)))
  :background  #(do
                  (let [v (ensure-sync-when-atom %1 :background %2)]
                    (.setBackground %1 (to-color v))
                    (.setOpaque %1 true)))
  :foreground  #(.setForeground %1 (to-color (ensure-sync-when-atom %1 :foreground %2)))
  :border      #(.setBorder %1 (to-border (ensure-sync-when-atom %1 :border %2)))
  :font        #(.setFont %1 (to-font (ensure-sync-when-atom %1 :font %2)))
  :tip         #(.setToolTipText %1 (str (ensure-sync-when-atom %1 :tip %2)))
  :text        #(.setText %1 (str (ensure-sync-when-atom %1 :text %2)))
  :icon        #(.setIcon %1 (make-icon (ensure-sync-when-atom %1 :icon %2)))
  :action      #(.setAction %1 (ensure-sync-when-atom %1 :action %2))
  :editable?   #(.setEditable %1 (boolean (ensure-sync-when-atom %1 :editable? %2)))
  :visible?    #(.setVisible %1 (boolean (ensure-sync-when-atom %1 :visible? %2)))
  :halign      #(.setHorizontalAlignment %1 (h-alignment-table %2))
  :valign      #(.setVerticalAlignment %1 (v-alignment-table %2)) 
  :orientation #(.setOrientation %1 (orientation-table (ensure-sync-when-atom %1 :orientation %2)))
  :items       #(add-widgets %1 %2)
  :model       #(.setModel %1 %2)
  :preferred-size #(.setPreferredSize %1 (to-dimension (ensure-sync-when-atom %1 :preferred-size %2)))
  :minimum-size   #(.setMinimumSize %1 (to-dimension (ensure-sync-when-atom %1 :minimum-size %2)))
  :maximum-size   #(.setMaximumSize %1 (to-dimension (ensure-sync-when-atom %1 :maximum-size %2)))
  :size           #(let [d (to-dimension %2)]
                     (doto %1 
                       (.setPreferredSize d)
                       (.setMinimumSize d)
                       (.setMaximumSize d)))
  :location   #(move! %1 :to %2)
  :bounds     bounds-option-handler
  :popup      #(popup-option-handler %1 %2)
})

(extend-type java.util.EventObject ConfigureWidget 
  (config* [target args] (config* (to-widget target false) args)))

(extend-type java.awt.Component ConfigureWidget 
  (config* [target args] 
    (reapply-options target args default-options)))

(extend-type javax.swing.JComponent ConfigureWidget 
  (config* [target args] 
    (reapply-options target args default-options)))

(extend-type Action ConfigureWidget 
  (config* [target args] 
    (reapply-options target args default-options)))

(extend-type java.awt.Window ConfigureWidget 
  (config* [target args] 
    (reapply-options target args default-options)))

(defn apply-default-opts
  "only used in tests!"
  ([p] (apply-default-opts p {}))
  ([^javax.swing.JComponent p {:as opts}]
    (apply-options p opts default-options)))

;*******************************************************************************
; ToDocument

; TODO ToDocument protocol
(defn to-document
  [v]
  (let [w (to-widget v)]
    (cond
      (instance? javax.swing.text.Document v)       v
      (instance? javax.swing.event.DocumentEvent v) (.getDocument v)
      (instance? JTextComponent w)                  (.getDocument w))))

;*******************************************************************************
; Null Layout

(defn xyz-panel
  "Creates a JPanel on which widgets can be positioned arbitrarily by client
  code. No layout manager is installed. 

  Initial widget positions can be given with their :bounds property. After
  construction they can be moved with the (seesaw.core/move!) function.

  Examples:

    ; Create a panel with a label positions at (10, 10) with width 200 and height 40.
    (xyz-panel :items [(label :text \"The Black Lodge\" :bounds [10 10 200 40]))

    ; Move a widget up 50 pixels and right 25 pixels
    (move! my-label :by [25 -50])

  Notes:
    This function is compatible with (seesaw.core/with-widget).

  See:
    (seesaw.core/move!)
  "
  [& opts]
  (let [p (construct JPanel)]
    (doto p
      (.setLayout nil)
      (apply-default-opts opts))))

;*******************************************************************************
; Border Layout

(def ^{:private true}  border-layout-dirs 
  (constant-map BorderLayout :north :south :east :west :center))

(defn- border-panel-items-handler
  [panel items]
  (doseq [[w dir] items]
    (add-widget panel w (border-layout-dirs dir))))

(def ^{:private true} border-layout-options 
  (merge
    { :hgap  #(.setHgap (.getLayout %1) %2)
      :vgap  #(.setVgap (.getLayout %1) %2) 
      :items border-panel-items-handler }
    (reduce 
      (fn [m [k v]] (assoc m k #(add-widget %1 %2 v)))
      {} 
      border-layout-dirs)))

(defn border-panel
  "Create a panel with a border layout. In addition to the usual options, 
  supports:
    
    :north  widget for north position (passed through to-widget)
    :south  widget for south position (passed through to-widget)
    :east   widget for east position (passed through to-widget)
    :west   widget for west position (passed through to-widget)
    :center widget for center position (passed through to-widget)
 
    :hgap   horizontal gap between widgets
    :vgap   vertical gap between widgets

  The :items option is a list of widget/direction pairs which can be used
  if you don't want to use the direction options directly. For example, both
  of these are equivalent:

    (border-panel :north \"North\" :south \"South\")

  is the same as:

    (border-panel :items [[\"North\" :north] [\"South\" :south]])

  This is for consistency with other containers.

  See:
  
    http://download.oracle.com/javase/6/docs/api/java/awt/BorderLayout.html
  "
  [& opts]
  (let [p (construct JPanel)]
    (.setLayout p (BorderLayout.))
    (apply-options p opts (merge default-options border-layout-options))))

;*******************************************************************************
; Flow

(def ^{:private true} flow-align-table
  (constant-map FlowLayout :left :right :leading :trailing :center))

(def ^{:private true} flow-panel-options {
  :hgap #(.setHgap (.getLayout %1) %2)
  :vgap #(.setVgap (.getLayout %1) %2)
  :align #(.setAlignment (.getLayout %1) (get flow-align-table %2 %2))
  :align-on-baseline? #(.setAlignOnBaseline (.getLayout %1) (boolean %2))
})

(defn flow-panel
  "Create a panel with a flow layout. Options:

    :items  List of widgets (passed through to-widget)
    :hgap   horizontal gap between widgets
    :vgap   vertical gap between widgets
    :align  :left, :right, :leading, :trailing, :center
    :align-on-baseline? 

  See http://download.oracle.com/javase/6/docs/api/java/awt/FlowLayout.html 
  "
  [& opts]
  (let [p (construct JPanel)]
    (.setLayout p (FlowLayout.))
    (apply-options p opts (merge default-options flow-panel-options))))

;*******************************************************************************
; Boxes

(def ^{:private true} box-layout-dir-table {
  :horizontal BoxLayout/X_AXIS 
  :vertical   BoxLayout/Y_AXIS 
})

(defn box-panel
  [dir & opts]
  (let [panel  (construct JPanel)
        layout (BoxLayout. panel (dir box-layout-dir-table))]
    (.setLayout panel layout)
    (apply-options panel opts default-options)))

(defn horizontal-panel 
  "Create a panel where widgets are arranged horizontally. Options:

    :items List of widgets (passed through to-widget)

  See http://download.oracle.com/javase/6/docs/api/javax/swing/BoxLayout.html 
  "
  [& opts] (apply box-panel :horizontal opts))

(defn vertical-panel
  "Create a panel where widgets are arranged vertically Options:

    :items List of widgets (passed through to-widget)

  See http://download.oracle.com/javase/6/docs/api/javax/swing/BoxLayout.html 
  "
  [& opts] (apply box-panel :vertical opts))

;*******************************************************************************
; Grid

(def ^{:private true} grid-panel-options {
  :hgap #(.setHgap (.getLayout %1) %2)
  :vgap #(.setVgap (.getLayout %1) %2)
})

(defn grid-panel
  "Create a panel where widgets are arranged horizontally. Options:
    
    :rows    Number of rows, defaults to 0, i.e. unspecified.
    :columns Number of columns.
    :items   List of widgets (passed through to-widget)
    :hgap    horizontal gap between widgets
    :vgap    vertical gap between widgets

  Note that it's usually sufficient to just give :columns and ignore :rows.

  See http://download.oracle.com/javase/6/docs/api/java/awt/GridLayout.html 
  "
  [& {:keys [rows columns] 
      :as opts}]
  (let [columns* (or columns (if rows 0 1))
        layout   (GridLayout. (or rows 0) columns* 0 0)
        panel    (construct JPanel)]
    (.setLayout panel layout)
    (apply-options panel 
      (dissoc opts :rows :columns) (merge default-options grid-panel-options))))

;*******************************************************************************
; Form aka GridBagLayout

(def ^{:private true} gbc-fill 
  (constant-map GridBagConstraints :none :both :horizontal :vertical))

(def ^{:private true} gbc-grid-xy (constant-map GridBagConstraints :relative))

(def ^{:private true} gbc-grid-wh
  (constant-map GridBagConstraints :relative :remainder))

(def ^{:private true} gbc-anchors 
  (constant-map GridBagConstraints
    :north :south :east :west 
    :northwest :northeast :southwest :southeast :center
    
    :page-start :page-end :line-start :line-end 
    :first-line-start :first-line-end :last-line-start :last-line-end
  
    :baseline :baseline-leading :baseline-trailing
    :above-baseline :above-baseline-leading :above-baseline-trailing
    :below-baseline :below-baseline-leading :below-baseline-trailing)) 

(defn- gbc-grid-handler [gbc v]
  (let [x (.gridx gbc)
        y (.gridy gbc)]
    (condp = v
      :next (set! (. gbc gridx) (inc x))
      :wrap    (do 
                 (set! (. gbc gridx) 0)
                 (set! (. gbc gridy) (inc y))))
    gbc))

(def ^{:private true} grid-bag-constraints-options {
  :grid       gbc-grid-handler
  :gridx      #(set! (. %1 gridx)      (get gbc-grid-xy %2 %2))
  :gridy      #(set! (. %1 gridy)      (get gbc-grid-xy %2 %2))
  :gridwidth  #(set! (. %1 gridwidget) (get gbc-grid-wh %2 %2))
  :gridheight #(set! (. %1 gridheight) (get gbc-grid-wh %2 %2))
  :fill       #(set! (. %1 fill)       (get gbc-fill %2 %2))
  :ipadx      #(set! (. %1 ipadx)      %2)
  :ipady      #(set! (. %1 ipady)      %2)
  :insets     #(set! (. %1 insets)     %2)
  :anchor     #(set! (. %1 anchor)     (gbc-anchors %2))
  :weightx    #(set! (. %1 weightx)    %2)
  :weighty    #(set! (. %1 weighty)    %2)
})

(defn realize-grid-bag-constraints
  "Turn item specs into [widget constraint] pairs by successively applying
  options to GridBagConstraints"
  [items]
  (second
    (reduce
      (fn [[gbcs result] [widget & opts]]
        (apply-options gbcs opts grid-bag-constraints-options)
        (vector (.clone gbcs) (conj result [widget gbcs]))) 
      [(GridBagConstraints.) []]
      items)))

(defn- add-grid-bag-items
  [panel items]
  (.removeAll panel)
  (doseq [[widget constraints] (realize-grid-bag-constraints items)]
    (when widget
      (add-widget panel widget constraints)))
  (handle-structure-change panel))

(def ^{:private true} form-panel-options {
  :items add-grid-bag-items
})

(defn form-panel
  "*Don't use this. GridBagLaout is an abomination*

  A panel that uses a GridBagLayout. Also aliased as (grid-bag-panel) if you
  want to be reminded of GridBagLayout. The :items property should be a list
  of vectors of the form:

      [widget & options]

  where widget is something widgetable and options are key/value pairs
  corresponding to GridBagConstraints fields. For example:

    [[\"Name\"         :weightx 0]
     [(text :id :name) :weightx 1 :fill :horizontal]]

  This creates a label/field pair where the field expands.
  
  See http://download.oracle.com/javase/6/docs/api/java/awt/GridBagLayout.html 
  "
  [& opts]
  (let [^java.awt.Container p (construct JPanel)]
    (.setLayout p (GridBagLayout.))
    (apply-options p opts (merge default-options form-panel-options))))

(def grid-bag-panel form-panel)

;*******************************************************************************
; MigLayout
(defn- apply-mig-constraints [widget constraints]
  (let [layout (.getLayout widget)
        [lc cc rc] constraints]
    (cond-doto layout
      lc (.setLayoutConstraints lc)
      cc (.setColumnConstraints cc)
      rc (.setRowConstraints rc))))

(defn- add-mig-items [parent items]
  (.removeAll parent)
  (doseq [[widget constraint] items]
    (add-widget parent widget constraint))
  (handle-structure-change parent))

(def ^{:private true} mig-panel-options {
  :constraints apply-mig-constraints
  :items       add-mig-items
})

(defn mig-panel
  "Construct a panel with a MigLayout. Takes one special property:

      :constraints [\"layout constraints\" \"column constraints\" \"row constraints\"]

  These correspond to the three constructor arguments to MigLayout.
  A vector of 0, 1, 2, or 3 constraints can be given.

  The format of the :items property is a vector of [widget, constraint] pairs.
  For example:

    :items [[ \"Propeller\"        \"split, span, gaptop 10\"]]

  See http://www.miglayout.com
  "
  [& opts]
  (let [p (construct JPanel)]
    (.setLayout p (net.miginfocom.swing.MigLayout.))
    (apply-options p opts (merge default-options mig-panel-options))))

;*******************************************************************************
; Labels

(def ^{:private true} label-options {
  :h-text-position #(.setHorizontalTextPosition %1 (h-alignment-table %2))
  :v-text-position #(.setVerticalTextPosition %1 (v-alignment-table %2))
})

(defn label 
  "Create a label. Supports all default properties. Can take two forms:

      (label \"My Label\")  ; Single text argument for the label

  or with full options:

      (label :id :my-label :text \"My Label\" ...)

  Additional options:

    :h-text-position Horizontal text position, :left, :right, :center, etc.
    :v-text-position Horizontal text position, :top, :center, :bottom, etc.

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JLabel.html
  "
  [& args]
  (case (count args) 
    0 (label :text "")
    1 (label :text (first args))
    (apply-options (construct JLabel) args (merge default-options label-options))))


;*******************************************************************************
; Buttons

(def ^{:private true} button-group-options {
  :buttons #(doseq [b %2] (.add %1 b))
})

(defn button-group
  "Creates a button group, i.e. a group of mutually exclusive toggle buttons, 
  radio buttons, toggle-able menus, etc. Takes the following options:

    :buttons A sequence of buttons to include in the group. They are *not*
             passed through (to-widget), i.e. they must be button or menu 
             instances.

  The mutual exclusion of the buttons in the group will be maintained automatically.
  The currently \"selected\" button can be retrieved and set with (selection) and
  (selection!) as usual.

  Note that a button can be added to a group when the button is created using the
  :group option of the various button and menu creation functions.

  Examples:

    (let [bg (button-group)]
      (flow-panel :items [(radio :id :a :text \"A\" :group bg)
                          (radio :id :b :text \"B\" :group bg)]))

    ; now A and B are mutually exclusive

    ; Check A
    (selection bg (select root [:#a]))

  Returns an instance of javax.swing.ButtonGroup

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/ButtonGroup.html
  "
  [& opts]
  (apply-options (ButtonGroup.) opts button-group-options))

(defmethod ^{:private true} setup-property-change-on-atom [javax.swing.JToggleButton :selected?]
  [component _ a]
  (listen component
          :change
          (fn [e]
            (reset! a (.isSelected component)))))

(def ^{:private true} button-options {
  :selected?   #(.setSelected %1 (boolean (ensure-sync-when-atom %1 :selected? %2)))
  :group       #(.add %2 %1)
})

(defn- apply-button-defaults
  ([button args] (apply-button-defaults button args {}))
  ([button args custom-options]
    (apply-options button args (merge default-options button-options custom-options))))

(defn button   [& args] (apply-button-defaults (construct JButton) args))
(defn toggle   [& args] (apply-button-defaults (construct JToggleButton) args))
(defn checkbox [& args] (apply-button-defaults (construct JCheckBox) args))
(defn radio    [& args] (apply-button-defaults (construct JRadioButton) args))

;*******************************************************************************
; Text widgets
(def ^{:private true} text-options {
  ; TODO split into single/multi options since some of these will fail if
  ; multi-line? is false  
  :columns     #(.setColumns %1 %2) 
  :rows        #(.setRows    %1 %2)
  :wrap-lines? #(doto %1 (.setLineWrap (boolean %2)) (.setWrapStyleWord (boolean %2)))
  :tab-size    #(.setTabSize %1 %2)
})

(defn text
  "Create a text field or area. Given a single argument, creates a JTextField 
  using the argument as the initial text value. Otherwise, supports the 
  following additional properties:

    :text         Initial text content
    :multi-line?  If true, a JTextArea is created (default false)
    :editable?    If false, the text is read-only (default true)
    :wrap-lines?  If true (and :multi-line? is true) lines are wrapped. 
                  (default false)
    :tab-size     Tab size in spaces. Defaults to 8.
    :rows         Number of rows if :multi-line? is true (default 0).

  To listen for document changes, use the :listen option:

    (text :listen [:document #(... handler ...)])

  or attach a listener later with (listen):
    
    (text :id :my-text ...)
        ...
    (listen (select root [:#my-text]) :document #(... handler ...))

  Given a single widget or document (or event) argument, retrieves the
  text of the argument. For example:

      user=> (def t (text \"HI\"))
      user=> (text t)
      \"HI\"

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JTextArea.html 
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JTextField.html 
  " 
  [& args]
  ; TODO this is crying out for a multi-method or protocol
  (let [one?        (= (count args) 1)
        [arg0 arg1] args
        as-doc      (to-document arg0)
        as-widget   (to-widget arg0)
        multi?      (or (coll? arg0) (seq? arg0))]
    (cond
      (and one? (nil? arg0)) (throw (IllegalArgumentException. "First arg must not be nil"))
      (and one? as-doc)      (.getText as-doc 0 (.getLength as-doc))
      (and one? as-widget)   (.getText as-widget)
      (and one? multi?)      (map #(text %) arg0)
      one?                   (text :text arg0)

      :else (let [{:keys [multi-line?] :as opts} args
                  t (if multi-line? (construct JTextArea) (construct JTextField))]
              (apply-options t 
                (dissoc opts :multi-line?)
                (merge default-options text-options))))))

(defn text!
  "Set the text of widget(s) or document(s). targets is an object that can be
  turned into a widget or document, or a list of such things. value is the new
  text value to be applied. Returns targets.

  Example:

      user=> (def t (text \"HI\"))
      user=> (text! t \"BYE\")
      user=> (text t)
      \"BYE\"

  "
  [targets value]
  (let [as-doc      (to-document targets)
        as-widget   (to-widget targets)
        multi?      (or (coll? targets) (seq? targets))]
    (cond
      (nil? targets) (throw (IllegalArgumentException. "First arg must not be nil"))
      as-doc      (do (.replace as-doc 0 (.getLength as-doc) value nil) as-doc)
      as-widget   (do (.setText as-widget value) as-widget)
      multi?      (do (doseq [w targets] (text w value)) targets))))

;*******************************************************************************
; JPasswordField

(def ^{:private true} password-options (merge {
  :echo-char #(.setEchoChar %1 %2)
} text-options))

(defn password
  "Create a password field. Options are the same as single-line text fields with
  the following additions:

    :echo-char The char displayed for the characters in the password field

  Returns an instance of JPasswordField.

  Example:

    (password :echo-char \\X)

  Notes:
    This function is compatible with (seesaw.core/with-widget).

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JPasswordField.html
  "
  [& opts]
  (let [pw (construct javax.swing.JPasswordField)]
    (apply-options pw opts (merge password-options default-options))))

(defn with-password*
  "Retrieve the password of a password field and passes it to the given handler
  function as an array or characters. Upon completion, the array is zero'd out
  and the value returned by the handler is returned.

  This is the 'safe' way to access the password. The (text) function will work too
  but that method is discouraged, at least by the JPasswordField docs.

  Example:

    (with-password* my-password-field
      (fn [password-chars]
        (... do something with chars ...)))

  See:
    (seesaw.core/password)
    (seesaw.core/text)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JPasswordField.html
  "
  [^javax.swing.JPasswordField field handler]
  (let [chars (.getPassword field)]
    (try
      (handler chars)
      (finally
        (java.util.Arrays/fill chars \0)))))

;*******************************************************************************
; JEditorPane

(def ^{:private true} editor-pane-options {
  :page         #(.setPage %1 (if (instance? java.net.URL %2) %2 (str %1)))
  :content-type #(.setContentType %1 (str %2))
  :editor-kit   #(.setEditorKit %1 %2)
})

(defn editor-pane
  "Create a JEditorPane. Custom options:

    :page         A URL (string or java.net.URL) with the contents of the editor
    :content-type The content-type, for example \"text/html\" for some crappy
                  HTML rendering.
    :editor-kit   The EditorKit. See Javadoc.

  Notes:
    This function is compatible with (seesaw.core/with-widget).

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JEditorPane.html
  "
  [& opts]
  (apply-options (construct javax.swing.JEditorPane) opts (merge default-options text-options)))

;*******************************************************************************
; Listbox

(defn- to-list-model [xs]
  (if (instance? javax.swing.ListModel xs)
    xs
    (let [model (javax.swing.DefaultListModel.)]
      (doseq [x xs]
        (.addElement model x))
      model)))

(def ^{:private true} listbox-options {
  :model    (fn [lb m] ((:model default-options) lb (to-list-model m)))
  :renderer #(.setCellRenderer %1 (cells/to-cell-renderer %1 %2))
})

(defn listbox
  "Create a list box (JList). Additional options:

    :model A ListModel, or a sequence of values with which a DefaultListModel
           will be constructed.
    :renderer A cell renderer to use. See (seesaw.cells/to-cell-renderer).

  Notes:
    This function is compatible with (seesaw.core/with-widget).

    Retrieving and setting the current selection of the list box is fully 
    supported by the (selection) and (selection!) functions.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JList.html 
  "
  [& args]
  (apply-options (construct javax.swing.JList) args (merge default-options listbox-options)))

;*******************************************************************************
; JTable

(defn- to-table-model [v]
  (cond
    (instance? javax.swing.table.TableModel v) v
    :else (apply seesaw.table/table-model v)))

(def ^{:private true} table-options {
  :model      #(.setModel %1 (to-table-model %2))
  :show-grid? #(.setShowGrid %1 (boolean %2))
  :fills-viewport-height? #(.setFillsViewportHeight %1 (boolean %2))
})

(defn table
  "Create a table (JTable). Additional options:

    :model A TableModel, or a vector. If a vector, then it is used as
           arguments to (seesaw.table/table-model).

  Example:

    (table 
      :model [:columns [:age :height]
              :rows    [{:age 13 :height 45}
                        {:age 45 :height 13}]])

  Notes:
    This function is compatible with (seesaw.core/with-widget).

  See:
    seesaw.table/table-model 
    seesaw.examples.table
    http://download.oracle.com/javase/6/docs/api/javax/swing/JTable.html"
  [& args]
  (apply-options 
    (doto (construct javax.swing.JTable)
      (.setFillsViewportHeight true)) args (merge default-options table-options)))

;*******************************************************************************
; JTree

(def ^{:private true} tree-options {
  :renderer #(.setCellRenderer %1 (cells/to-cell-renderer %1 %2))
  :expands-selected-paths? #(.setExpandsSelectedPaths %1 (boolean %2))
  :large-model?            #(.setLargeModel %1 (boolean %2))
  :root-visible?           #(.setRootVisible %1 (boolean %2))
  :row-height              #(.setRowHeight %1 %2)
  :scrolls-on-expand?      #(.setScrollsOnExpand %1 (boolean %2))
  :shows-root-handles?     #(.setShowsRootHandles %1 (boolean %2))
  :toggle-click-count      #(.setToggleClickCount %1 %2)
  :visible-row-count       #(.setVisibleRowCount %1 %2)
})

(defn tree
  "Create a tree (JTree). Additional options:

  Notes:
    This function is compatible with (seesaw.core/with-widget).

  See:
  
    http://download.oracle.com/javase/6/docs/api/javax/swing/JTree.html
  "
  [& args]
  (apply-options (construct javax.swing.JTree) args (merge default-options tree-options)))

;*******************************************************************************
; Combobox

(defn- to-combobox-model [xs]
  (if (instance? javax.swing.ComboBoxModel xs)
    xs
    (let [model (javax.swing.DefaultComboBoxModel.)]
      (doseq [x xs]
        (.addElement model x))
      (when (seq xs)
        (.setSelectedItem model (first xs)))
      model)))

(def ^{:private true} combobox-options {
  :model    (fn [lb m] ((:model default-options) lb (to-combobox-model m)))
  :renderer #(.setRenderer %1 (cells/to-cell-renderer %1 %2))
})

(defn combobox
  "Create a combo box (JComboBox). Additional options:

    :model Instance of ComboBoxModel, or sequence of values used to construct
           a default model.
    :renderer Cell renderer used for display. See (seesaw.cells/to-cell-renderer).

  Note that the current selection can be retrieved and set with the (selection) and
  (selection!) functions.

  Notes:
    This function is compatible with (seesaw.core/with-widget).

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JComboBox.html
  "
  [& args]
  (apply-options (construct javax.swing.JComboBox) args (merge default-options combobox-options)))

;*******************************************************************************
; Scrolling

(def ^{:private true} hscroll-table {
  :as-needed  ScrollPaneConstants/HORIZONTAL_SCROLLBAR_AS_NEEDED
  :never      ScrollPaneConstants/HORIZONTAL_SCROLLBAR_NEVER
  :always     ScrollPaneConstants/HORIZONTAL_SCROLLBAR_ALWAYS 
})
(def ^{:private true} vscroll-table {
  :as-needed  ScrollPaneConstants/VERTICAL_SCROLLBAR_AS_NEEDED
  :never      ScrollPaneConstants/VERTICAL_SCROLLBAR_NEVER
  :always     ScrollPaneConstants/VERTICAL_SCROLLBAR_ALWAYS 
})

(def ^{:private true} scrollable-options {
  :hscroll #(.setHorizontalScrollBarPolicy %1 (hscroll-table %2))
  :vscroll #(.setVerticalScrollBarPolicy %1 (vscroll-table %2))
})

(defn scrollable 
  "Wrap target in a JScrollPane and return the scroll pane.

  The first argument is always the widget that should be scrolled. It's followed
  by zero or more options *for the scroll pane*.

  Additional Options:

    :hscroll - Controls appearance of horizontal scroll bar. 
               One of :as-needed (default), :never, :always
    :vscroll - Controls appearance of vertical scroll bar.
               One of :as-needed (default), :never, :always

  Examples:

    ; Vanilla scrollable
    (scrollable (listbox :model [\"Foo\" \"Bar\" \"Yum\"]))

    ; Scrollable with some options on the JScrollPane
    (scrollable (listbox :model [\"Foo\" \"Bar\" \"Yum\"]) :id :#scrollable :border 5)

  Notes:
    This function is compatible with (seesaw.core/with-widget).
  
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JScrollPane.html
  "
  [target & opts]
  (let [sp (construct JScrollPane)]
    (.setViewportView sp (to-widget target true))
    (apply-options sp opts (merge default-options scrollable-options))))

;*******************************************************************************
; Splitter
(defn splitter
  [dir left right & opts]
  (apply-options
    (doto (construct JSplitPane)
      (.setOrientation (dir {:left-right JSplitPane/HORIZONTAL_SPLIT
                             :top-bottom JSplitPane/VERTICAL_SPLIT}))
      (.setLeftComponent (to-widget left true))
      (.setRightComponent (to-widget right true)))
    opts
    default-options))

(defn left-right-split 
  "Create a left/right (horizontal) splitpane with the given widgets.
  
  Notes:
    This function is compatible with (seesaw.core/with-widget).
  
  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JSplitPane.html
  "
  [left right & args] (apply splitter :left-right left right args))

(defn top-bottom-split 
  "Create a top/bottom (vertical) split pane with the given widgets
  
  Notes:
    This function is compatible with (seesaw.core/with-widget).
  
  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JSplitPane.html
  "
  [top bottom & args] (apply splitter :top-bottom top bottom args))

;*******************************************************************************
; Separator

(defn separator
  "Create a separator.

  Notes:
    This function is compatible with (seesaw.core/with-widget).
  
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JSeparator.html
  "
  [& opts]
  (apply-options (construct javax.swing.JSeparator) opts default-options))

;*******************************************************************************
; Menus

(def ^{:private true} menu-item-options {
  :key #(.setAccelerator %1 (seesaw.keystroke/keystroke %2))
})

(defn menu-item          [& args] (apply-button-defaults (javax.swing.JMenuItem.) args menu-item-options))
(defn checkbox-menu-item [& args] (apply-button-defaults (javax.swing.JCheckBoxMenuItem.) args))
(defn radio-menu-item    [& args] (apply-button-defaults (javax.swing.JRadioButtonMenuItem.) args))

(defn- to-menu-item
  [item]
  ; TODO this sucks
  (if (instance? javax.swing.Action item) 
    (javax.swing.JMenuItem. item)
    (if-let [icon (make-icon item)]
      (javax.swing.JMenuItem. icon)
      (if (instance? String item)
        (javax.swing.JMenuItem. item)
        (to-widget item true)))))

(def ^{:private true} menu-options {
  ;:items #(add-widgets %1 (map to-menu-item %2))
  :items #(doseq [item (map to-menu-item %2)] (.add %1 item))
})

(defn menu 
  "Create a new menu. Additional options:

    :items Sequence of menu item-like things (actions, icons, JMenuItems, etc)
  
  Notes:
    This function is compatible with (seesaw.core/with-widget).
  
  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JMenu.html"
  [& opts]
  (apply-button-defaults (construct javax.swing.JMenu) opts menu-options))

(defn popup 
  "Create a new popup menu. Additional options:

    :items Sequence of menu item-like things (actions, icons, JMenuItems, etc)

  Note that in many cases, the :popup option is what you want if you want to
  show a context menu on a widget. It handles all the yucky mouse stuff and
  fixes various eccentricities of Swing.
  
  Notes:
    This function is compatible with (seesaw.core/with-widget).
  
  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JPopupMenu.html"
  [& opts]
  (apply-options (construct javax.swing.JPopupMenu) opts (merge default-options menu-options)))


(defn- make-popup [target arg event]
  (cond
    (instance? javax.swing.JPopupMenu arg) arg
    (fn? arg)                              (popup :items (arg event))
    :else (throw (IllegalArgumentException. (str "Don't know how to make popup with " arg)))))

(defn- popup-option-handler
  [target arg]
  (listen target :mouse 
    (fn [event]
      (when (.isPopupTrigger event)
        (let [p (make-popup target arg event)]
          (.show p (to-widget event) (.x (.getPoint event)) (.y (.getPoint event))))))))

  
;(def ^{:private true} menubar-options {
  ;:items (fn [mb items] (doseq [i items] (.add mb i)))
;})

(defn menubar
  "Create a new menu bar, suitable for the :menubar property of (frame). 
  Additional options:

    :items Sequence of menus, see (menu).
  
  Notes:
    This function is compatible with (seesaw.core/with-widget).
  
  See:
    seesaw.core/frame
    http://download.oracle.com/javase/6/docs/api/javax/swing/JMenuBar.html
  "
  [& opts]
  (apply-options (construct javax.swing.JMenuBar) opts default-options))

;*******************************************************************************
; Toolbars


(defn- insert-toolbar-separators 
  "Replace :separator with JToolBar$Separator instances"
  [items]
  (map #(if (= % :separator) (javax.swing.JToolBar$Separator.) %) items))

(def ^{:private true} toolbar-options {
  :floatable? #(.setFloatable %1 (boolean %2))
  ; Override default :items handler
  :items     #(add-widgets %1 (insert-toolbar-separators %2))
})

(defn toolbar
  "Create a JToolBar. The following properties are supported:
 
    :floatable?  Whether the toolbar is floatable.
    :orientation Toolbar orientation, :horizontal or :vertical
    :items       Normal list of widgets to add to the toolbar. :separator
                 creates a toolbar separator.

  Notes:
    This function is compatible with (seesaw.core/with-widget).
  
  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JToolBar.html
  "
  [& opts]
  (apply-options (construct JToolBar) opts (merge default-options toolbar-options)))

;*******************************************************************************
; Tabs

(def ^{:private true} tab-placement-table
  (constant-map SwingConstants :bottom :top :left :right))

(def ^{:private true} tab-overflow-table {
  :scroll JTabbedPane/SCROLL_TAB_LAYOUT
  :wrap   JTabbedPane/WRAP_TAB_LAYOUT
})

(defn- add-to-tabbed-panel 
  [tp tab-defs]
  (doseq [{:keys [title content tip icon]} tab-defs]
    (let [title-cmp (try-cast Component title)
          index     (.getTabCount tp)]
      (cond-doto tp
        true (.addTab (when-not title-cmp (str title)) (make-icon icon) (to-widget content true) (str tip))
        title-cmp (.setTabComponentAt index title-cmp))))
  tp)

(def ^{:private true} tabbed-panel-options {
  :placement #(.setTabPlacement %1 (tab-placement-table %2))
  :overflow  #(.setTabLayoutPolicy %1 (tab-overflow-table %2))
  :tabs      add-to-tabbed-panel
})

(defn tabbed-panel
  "Create a JTabbedPane. Supports the following properties:

    :placement Tab placement, one of :bottom, :top, :left, :right.
    :overflow  Tab overflow behavior, one of :wrap, :scroll.
    :tabs      A list of tab descriptors. See below

  A tab descriptor is a map with the following properties:

    :title     Title of the tab or a component to be displayed.
    :tip       Tab's tooltip text
    :icon      Tab's icon, passed through (icon)
    :content   The content of the tab, passed through (to-widget) as usual.

  Returns the new JTabbedPane.

  Notes:
    This function is compatible with (seesaw.core/with-widget).
  
  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JToolBar.html
  "
  [& opts]
  (apply-options (construct JTabbedPane) opts (merge default-options tabbed-panel-options)))

;*******************************************************************************
; Canvas

(def ^{:private true} paint-property "seesaw-paint")

(defn- canvas-paint-option-handler [c v]
  (cond 
    (nil? v) (canvas-paint-option-handler c {:before nil :after nil :super? true})
    (fn? v)  (canvas-paint-option-handler c {:after v})
    (map? v) (do (put-meta! c paint-property v) (.repaint c))
    :else (throw (IllegalArgumentException. "Expect map or function for :paint property"))))

(def ^{:private true} canvas-options {
  :paint canvas-paint-option-handler
})

(defn- create-paintable []
  (proxy [javax.swing.JPanel] []
    (paintComponent [g]
      (let [{:keys [before after super?] :or {super? true}} (get-meta this paint-property)]
        (ssg/anti-alias g)
        (when before (ssg/push g (before this g)))
        (when super? (proxy-super paintComponent g))
        (when after  (ssg/push g (after this g)))))))

(defn canvas
  [& opts]
  "Creates a paintable canvas, i.e. a JPanel with paintComponent overridden. 
  Painting is configured with the :paint property which can be:

    nil - disables painting. The canvas' will be filled with its background
      color

    (fn [c g]) - a paint function that takes the canvas and a Graphics2D as 
      arguments. Called after super.paintComponent.

    {:before fn :after fn} - a map with :before and :after functions which
      are called before and after super.paintComponent respectively.
  
  Note that (config!) can be used to change the :paint property at any time.
  
  Here's an example:
  
    (canvas :paint #(.drawString %2 \"I'm a canvas\" 10 10))

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JComponent.html#paintComponent%28java.awt.Graphics%29 
  "
  (let [p (create-paintable)]
    (.setLayout p nil)
    (apply-options p opts (merge default-options canvas-options))))

;*******************************************************************************
; Frame

(def ^{:private true} frame-on-close-map {
  :hide    JFrame/HIDE_ON_CLOSE
  :dispose JFrame/DISPOSE_ON_CLOSE
  :exit    JFrame/EXIT_ON_CLOSE
  :nothing JFrame/DO_NOTHING_ON_CLOSE
})

(def ^{:private true} frame-options {
  :id           id-option-handler
  :title        #(.setTitle %1 (str %2))
  :resizable?   #(.setResizable %1 (boolean %2))
  :content      #(.setContentPane %1 (to-widget %2 true))
  :menubar      #(.setJMenuBar %1 %2)
  :minimum-size #(.setMinimumSize %1 (to-dimension %2))
  :size         #(.setSize %1 (to-dimension %2))
  :on-close     #(.setDefaultCloseOperation %1 (frame-on-close-map %2))
  :visible?     #(.setVisible %1 (boolean %2))
})

(defn frame
  "Create a JFrame. Options:

    :id       id of the window, used by (select).
    :title    the title of the window
    :width    initial width. Note that calling (pack!) will negate this setting
    :height   initial height. Note that calling (pack!) will negate this setting
    :size     initial size. Note that calling (pack!) will negate this setting
    :minimum-size minimum size of frame, e.g. [640 :by 480]
    :content  passed through (to-widget) and used as the frame's content-pane
    :visible?  whether frame should be initially visible (default false)
    :resizable? whether the frame can be resized (default true)
    :on-close   default close behavior. One of :exit, :hide, :dispose, :nothing

  returns the new frame.

  Examples:

    ; Create a frame, pack it and show it.
    (-> (frame :title \"HI!\" :content \"I'm a label!\")
      pack!
      show!)
      
    ; Create a frame with an initial size (note that pack! isn't called)
    (show! (frame :title \"HI!\" :content \"I'm a label!\" :width 500 :height 600))

  Notes:
    Unless :visible? is set to true, the frame will not be displayed until (show!)
    is called on it.

    Call (pack!) on the frame if you'd like the frame to resize itself to fit its
    contents. Sometimes this doesn't look like crap.

    This function is compatible with (seesaw.core/with-widget).
  
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JFrame.html 
  "
  [& {:keys [width height visible?] 
      :or {width 100 height 100}
      :as opts}]
  (cond-doto (apply-options (construct JFrame) 
               (dissoc opts :width :height :visible?) frame-options)
    true     (.setSize width height)
    true     (.setVisible (boolean visible?))))

(defn- get-root
  "Basically the same as SwingUtilities/getRoot, except handles JPopupMenus 
  by following the invoker of the popup if it doesn't have a parent. This
  allows (to-root) to work correctly on action event objects fired from
  menus.
  
  Returns top-level Window (e.g. a JFrame), or nil if not found."
  [w]
  (cond
    (nil? w) w
    (instance? java.awt.Window w) w
    (instance? java.applet.Applet w) w
    (instance? javax.swing.JPopupMenu w) 
      (if-let [p (.getParent w)] 
        (get-root p) 
        (get-root (.getInvoker w)))
    :else (get-root (.getParent w))))

(defn to-root
  "Get the frame or window that contains the given widget. Useful for APIs
  like JDialog that want a JFrame, when all you have is a widget or event.
  Note that w is run through (to-widget) first, so you can pass event object
  directly to this."
  [w]
  (get-root (to-widget w)))

(def to-frame to-root)

;*******************************************************************************
; Custom-Dialog

(def ^{:private true} dialog-modality-table {
  true         java.awt.Dialog$ModalityType/APPLICATION_MODAL
  false        java.awt.Dialog$ModalityType/MODELESS
  nil          java.awt.Dialog$ModalityType/MODELESS
  :application java.awt.Dialog$ModalityType/APPLICATION_MODAL
  :document    java.awt.Dialog$ModalityType/DOCUMENT_MODAL
  :toolkit     java.awt.Dialog$ModalityType/TOOLKIT_MODAL
})

(def ^{:private true} custom-dialog-options {
  :modal? #(.setModalityType %1 (or (dialog-modality-table %2) (dialog-modality-table (boolean %2))))
  :parent #(.setLocationRelativeTo %1 %2)
})

(def ^{:private true} dialog-result-property ::dialog-result)

(defn- is-modal-dialog? [dlg] 
  (and (instance? java.awt.Dialog dlg) 
       (not= (.getModalityType dlg) java.awt.Dialog$ModalityType/MODELESS)))

(defn- show-modal-dialog [dlg]
  {:pre [(is-modal-dialog? dlg)]}
  (let [dlg-result (atom nil)]
    (listen dlg
            :window-opened
            (fn [_] (put-meta! dlg dialog-result-property dlg-result))
            #{:window-closing :window-closed}
            (fn [_] (put-meta! dlg dialog-result-property nil)))
    (config! dlg :visible? true)
    @dlg-result))

(defn return-from-dialog
  "Return from the given dialog with the specified value. dlg may be anything
  that can be converted into a dialog as with (to-root). For example, an
  event, or a child widget of the dialog. Result is the value that will
  be returned from the blocking (dialog), (custom-dialog), or (show!)
  call.

  Examples:

    ; A button with an action listener that will cause the dialog to close
    ; and return :ok to the invoker.
    (button 
      :text \"OK\" 
      :listen [:action (fn [e] (return-from-dialog e :ok))])
  
  Notes:
    The dialog must be modal and created from within the DIALOG fn with both
    VISIBLE? and MODAL? set to true.
  "
  [dlg result]
  ;(assert-ui-thread "return-from-dialog")
  (let [dlg         (to-root dlg)
        result-atom (get-meta dlg dialog-result-property)]
    (if result-atom
      (do 
        (reset! result-atom result)
        (invoke-now (dispose! dlg)))
      (throw (IllegalArgumentException. "Counld not find dialog meta data!")))))

(defn custom-dialog
  "Create a dialog and display it.

      (custom-dialog ... options ...)

  Besides the default & frame options, options can also be one of:

    :parent  The window which the new dialog should be positioned relatively to.
    :modal?  A boolean value indicating whether this dialog is to be a
              modal dialog.  If :modal? *and* :visible? are set to
              true (:visible? is true per default), the function will
              block with a dialog. The function will return once the user:
              a) Closes the window by using the system window
                 manager (e.g. by pressing the \"X\" icon in many OS's)
              b) A function from within an event calls (dispose!) on the dialog
              c) A function from within an event calls RETURN-FROM-DIALOG
                  with a return value.
              In the case of a) and b), this function returns nil. In the
              case of c), this function returns the value passed to
              RETURN-FROM-DIALOG. Default: true.


  Returns a JDialog. Use (seesaw.core/show!) to display the dialog.

  Notes:
    This function is compatible with (seesaw.core/with-widget).
 
  See:
    (seesaw.core/show!)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JDialog.html
"
  [& {:keys [width height visible? modal? on-close] 
      :or {width 100 height 100 visible? false}
      :as opts}]
  (let [dlg (apply-options (construct JDialog) 
                           (merge {:modal? true} (dissoc opts :width :height :visible? :pack?))
                           (merge custom-dialog-options frame-options))]
    (.setSize dlg width height)
    (if visible?
      (show! dlg)
      dlg)))


;*******************************************************************************
; Alert
(defn alert
  "Show a simple message alert dialog. Take an optional parent component, source,
  used for dialog placement, and a message which is passed through (str).

  Examples:

    (alert \"Hello!\")
    (alert e \"Hello!\")

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JOptionPane.html#showMessageDialog%28java.awt.Component,%20java.lang.Object%29
  "
  ([source message] 
    (JOptionPane/showMessageDialog (to-widget source) (str message)))
  ([message] (alert nil message)))

;*******************************************************************************
; Input
(def ^{:private true} input-type-map {
  :error    JOptionPane/ERROR_MESSAGE
  :info     JOptionPane/INFORMATION_MESSAGE
  :warning  JOptionPane/WARNING_MESSAGE
  :question JOptionPane/QUESTION_MESSAGE
  :plain    JOptionPane/PLAIN_MESSAGE
})

(defrecord InputChoice [value to-string]
  Object
  (toString [this] (to-string value)))

(defn- input-impl
  "
    showInputDialog(Component parentComponent, 
                    Object message, 
                    String title, 
                    int messageType, 
                    Icon icon, 
                    Object[] selectionValues, 
                    Object initialSelectionValue) 
  "
  [source message {:keys [title value type choices icon to-string] 
                   :or {type :plain to-string str}}]
  (let [source  (to-widget source)
        message (if (coll? message) (object-array message) (str message))
        choices (when choices (object-array (map #(InputChoice. % to-string) choices)))
        result  (JOptionPane/showInputDialog ^java.awt.Component source 
                                 message 
                                 title 
                                 (input-type-map type) 
                                 (make-icon icon)
                                 choices value)]
    (if (and result choices)
      (.value result)
      result)))

(defn input
  "Show an input dialog:
    
    (input [source] message & options)

  source  - optional parent component
  message - The message to show the user. May be a string, or list of strings, widgets, etc.
  options - additional options

  Additional options:

    :title     The dialog title
    :value     The initial, default value to show in the dialog
    :choices   List of values to choose from rather than freeform entry
    :type      :warning, :error, :info, :plain, or :question
    :icon      Icon to display (Icon, URL, etc)
    :to-string A function which creates the string representation of the values 
               in :choices. This let's you choose arbitrary clojure data structures
               without while keeping things looking nice. Defaults to str.

  Examples:

    ; Ask for a string input
    (input \"Bang the keyboard like a monkey\")

    ; Ask for a choice from a set
    (input \"Pick a color\" :choices [\"RED\" \"YELLO\" \"GREEN\"])

    ; Choose from a list of maps using a custom string function for the display.
    ; This will display only the city names, but the return value will be one of
    ; maps in the :choices list. Yay!
    (input \"Pick a city\" 
      :choices [{ :name \"New York\"  :population 8000000 }
                { :name \"Ann Arbor\" :population 100000 }
                { :name \"Twin Peaks\" :population 5201 }]
      :to-string :name)

  Returns the user input or nil if they hit cancel.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JOptionPane.html
  "
  [& args]
  (let [n (count args)
        f (first args)
        s (second args)]
    (cond
      (or (= n 0) (keyword? f))
        (throw (IllegalArgumentException. "input requires at least one non-keyword arg"))
      (= n 1)      (input-impl nil f {})
      (= n 2)      (input-impl f s {})
      (keyword? s) (input-impl nil f (drop 1 args))
      :else        (input-impl f  s (drop 2 args)))))


;*******************************************************************************
; dialog
(def ^:private dialog-option-type-map {
  :default       JOptionPane/DEFAULT_OPTION
  :yes-no        JOptionPane/YES_NO_OPTION
  :yes-no-cancel JOptionPane/YES_NO_CANCEL_OPTION
  :ok-cancel     JOptionPane/OK_CANCEL_OPTION
})

(def ^:private dialog-defaults {
  :parent nil
  :content "Please set the :content option."
  :option-type :default
  :type :plain
  :options nil
  :default-option nil
  :success-fn (fn [_] :success)
  :cancel-fn (fn [_])
  :no-fn (fn [_] :no)
})

(defn dialog
  "Display a JOptionPane. This is a dialog which displays some
  input/question to the user, which may be answered using several
  standard button configurations or entirely custom ones.

      (dialog ... options ...)

  Options can be any of:

    :content      May be a string or a component (or a panel with even more 
                  components) which is to be displayed.

    :option-type  In case :options is *not* specified, this may be one of 
                  :default, :yes-no, :yes-no-cancel, :ok-cancel to specify 
                  which standard button set is to be used in the dialog.

    :type        The type of the dialog. One of :warning, :error, :info, :plain, or :question.

    :options     Custom buttons/options can be provided using this argument. 
                 It must be a seq of \"to-widget\"'able objects which will be 
                 displayed as options the user can choose from. Note that in this
                 case, :success-fn, :cancel-fn & :no-fn will *not* be called. 
                 Use the handlers on those buttons & RETURN-FROM-DIALOG to close 
                 the dialog.

    :default-option  The default option instance which is to be selected. This should be an element
                     from the :options seq.

    :success-fn  A function taking the JOptionPane as its only
                 argument. It will be called when no :options argument
                 has been specified and the user has pressed any of the \"Yes\" or \"Ok\" buttons.
                 Default: a function returning :success.
 
    :cancel-fn   A function taking the JOptionPane as its only
                 argument. It will be called when no :options argument
                 has been specified and the user has pressed the \"Cancel\" button.
                 Default: a function returning nil.

    :no-fn       A function taking the JOptionPane as its only
                 argument. It will be called when no :options argument
                 has been specified and the user has pressed the \"No\" button.
                 Default: a function returning :no.

  Any remaining options will be passed to dialog.

  Examples:

    ; display a dialog with only an \"Ok\" button.
    (dialog :content \"You may now press Ok\")

    ; display a dialog to enter a users name and return the entered name.
    (dialog :content
     (flow-panel :items [\"Enter your name\" (text :id :name :text \"Your name here\")])
                 :options-type :ok-cancel
                 :success-fn (fn [p] (.getText (select (to-root p) [:#name]))))

  The dialog is not immediately shown. Use (seesaw.core/show!) to display the dialog.
  If the dialog is model this will return the result of :success-fn, :cancel-fn or 
  :no-fn depending on what button the user pressed. 
  
  Alternatively if :options has been specified, returns the value which has been 
  passed to (seesaw.core/return-from-dialog).
"
  [& {:as opts}]
  ;; (Object message, int messageType, int optionType, Icon icon, Object[] options, Object initialValue)
  (let [{:keys [content option-type type
                options default-option success-fn cancel-fn no-fn]}
        (merge dialog-defaults opts) 
        pane (JOptionPane. 
              content 
              (input-type-map type)
              (dialog-option-type-map option-type)
              nil                       ;icon
              (when options
                (into-array (map #(to-widget % true) options)))
              (or default-option (first options)) ; default selection
              )]
    (let [dispatch-fns   {:yes-no        [success-fn no-fn]
                          :yes-no-cancel [success-fn no-fn cancel-fn]
                          :ok-cancel     [success-fn cancel-fn]
                          :default       [success-fn]}
          visible?       (get opts :visible? false) 
          remaining-opts (reduce dissoc opts (conj (keys dialog-defaults) :visible?)) 
          dlg            (apply custom-dialog (reduce concat [:visible? false :content pane] remaining-opts))]
      ;; when there was no options specified, default options will be
      ;; used, so the success-fn cancel-fn & no-fn must be called
      (when-not options
        (listen pane
                :property-change
                (fn [e] (when (and (.isVisible dlg)
                                   (= (.getPropertyName e) JOptionPane/VALUE_PROPERTY))
                          (return-from-dialog e ((get-in dispatch-fns
                                                       [option-type (.getValue pane)]
                                                       (fn [_] (println "No fn found for option-type:" option-type "and button id:" (.getValue pane))))
                                               pane))))))
      (if visible?
        (show! dlg)
        dlg))))


;*******************************************************************************
; Slider
(defmethod ^{:private true} setup-property-change-on-atom [javax.swing.JSlider :value]
  [component _ a]
  (listen component
          :change
          (fn [e]
            (reset! a (.getValue component)))))

(def ^{:private true} slider-options {
  :orientation #(.setOrientation %1 (or (orientation-table %2)
                                        (throw (IllegalArgumentException. (str ":orientation must be either :horizontal or :vertical. Got " %2 " instead.")))))
  :value #(let [v (ensure-sync-when-atom %1 :value %2)]
            (check-args (number? v) ":value must be a number or an atom.")
            (.setValue %1 v))
  :min #(do (check-args (number? %2) ":min must be a number.")
            (.setMinimum %1 %2))
  :max #(do (check-args (number? %2) ":max must be a number.")
            (.setMaximum %1 %2))
  :minor-tick-spacing #(do (check-args (number? %2) ":minor-tick-spacing must be a number.")
                           (.setPaintTicks %1 true)
                           (.setMinorTickSpacing %1 %2))
  :major-tick-spacing #(do (check-args (number? %2) ":major-tick-spacing must be a number.")
                           (.setPaintTicks %1 true)
                           (.setMajorTickSpacing %1 %2))
  :snap-to-ticks? #(.setSnapToTicks %1 (boolean %2))
  :paint-ticks? #(.setPaintTicks %1 (boolean %2))
  :paint-labels? #(.setPaintLabels %1 (boolean %2))
  :paint-track? #(.setPaintTrack %1 (boolean %2))
  :inverted? #(.setInverted %1 (boolean %2))
 
})

(defn slider
  "Show a slider which can be used to modify a value.

      (slider ... options ...)

  Besides the default options, options can also be one of:

    :orientation   The orientation of the slider. One of :horizontal, :vertical.
    :value         The initial numerical value that is to be set. This may be an
                   atom, in which case the atom will be kept in sync with the slider.
    :min           The minimum numerical value which can be set.
    :max           The maximum numerical value which can be set.
    :minor-tick-spacing  The spacing between minor ticks. If set, will also set :paint-ticks? to true.
    :major-tick-spacing  The spacing between major ticks. If set, will also set :paint-ticks? to true.
    :snap-to-ticks?  A boolean value indicating whether the slider should snap to ticks.
    :paint-ticks?    A boolean value indicating whether to paint ticks.
    :paint-labels?   A boolean value indicating whether to paint labels for ticks.
    :paint-track?    A boolean value indicating whether to paint the track.
    :inverted?       A boolean value indicating whether to invert the slider (to go from high to low).

  Returns a JSlider.

  Examples:

    ; ask & return single file
    (slider :value 10 :min -50 :max 50)

  Notes:
    This function is compatible with (seesaw.core/with-widget).
 
  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JSlider.html

"
  [& {:keys [orientation value min max minor-tick-spacing major-tick-spacing
             snap-to-ticks? paint-ticks? paint-labels? paint-track? inverted?]
      :as kw}] 
  (let [sl (construct javax.swing.JSlider)]
    (apply-options sl kw (merge default-options slider-options))))


;*******************************************************************************
; Progress Bar
(def ^{:private true} progress-bar-options {
  :orientation #(.setOrientation %1 (or (orientation-table %2)
                                        (throw (IllegalArgumentException. (str ":orientation must be either :horizontal or :vertical. Got " %2 " instead.")))))
  :value #(cond (atom? %2)
                  (ssb/bind-atom-to-range-model %2 (.getModel %1))
                (number? %2)
                  (.setValue %1 %2)
                :else
                  (throw (IllegalArgumentException. ":value must be a number or an atom.")))
  :min #(do (check-args (number? %2) ":min must be a number.")
            (.setMinimum %1 %2))
  :max #(do (check-args (number? %2) ":max must be a number.")
            (.setMaximum %1 %2))
  :visible? #(.setVisible %1 (boolean %2))
  :paint-string? #(.setStringPainted %1 (boolean %2))
  :indeterminate? #(.setIndeterminate %1 (boolean %2))
})

(defn progress-bar
  "Show a progress-bar which can be used to display the progress of long running tasks.

      (progress-bar ... options ...)

  Besides the default options, options can also be one of:

    :orientation   The orientation of the progress-bar. One of :horizontal, :vertical. Default: :horizontal.
    :value         The initial numerical value that is to be set. This may be an
                   atom, in which case the atom will be kept in sync with the slider. Default: 0.
    :min           The minimum numerical value which can be set. Default: 0.
    :max           The maximum numerical value which can be set. Default: 100.
    :paint-string? A boolean value indicating whether to paint a string containing
                   the progress' percentage. Default: false.
    :indeterminate? A boolean value indicating whether the progress bar is to be in
                    indeterminate mode (for when the exact state of the task is not
                    yet known). Default: false.

  Examples:

    ; vertical progress bar from 0 to 100 starting with inital value at 15.
    (progress-bar :orientation :vertical :min 0 :max 100 :value 15)

  Returns a JProgressBar.

  Notes:
    This function is compatible with (seesaw.core/with-widget).
 
  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JProgressBar.html

"
  [& {:keys [orientation value min max] :as kw}]
  (let [sl (construct javax.swing.JProgressBar)]
    (apply-options sl kw (merge default-options progress-bar-options))))



;*******************************************************************************
; Selectors
(def ^{:private true} id-regex #"^#(.+)$")

(defn select
  "Select a widget using the given selector expression. Selectors are *always*
   expressed as a vector. root is the root of the widget hierarchy to select
   from, usually either a (frame) or other container.

    (select root [:#id])   Look up widget by id. A single widget is returned
    (select root [:*])     root and all the widgets under it

   For example, to find a widget by id from an event handler, use (to-root) on
   the event to get the root:

    (fn [e]
      (let [my-widget (select (to-root e) [:#my-widget])]
         ...))

   Someday more selectors will be supported :)
  "
  ([root selector]
    (check-args (vector? selector) "selector must be vector")
    (if-let [[_ id] (re-find id-regex (name (first selector)))]
      ; TODO do some memoization of this rather than always searching the
      ; entire tree.
      (some #(when (= id (id-for %)) %) (select (to-widget root) [:*]))
      (cond
        (= (first selector) :*) (collect (to-widget root))
        :else (throw (IllegalArgumentException. (str "Unsupported selector " selector)))))))

;*******************************************************************************
; Widget layout manipulation

(defprotocol LayoutManipulation
  (add!* [layout target widget constraint])
  (get-constraint [layout container widget]))

(extend-protocol LayoutManipulation
  java.awt.LayoutManager
    (add!* [layout target widget constraint]
      (add-widget target widget))
    (get-constraint [layout container widget] nil)

  java.awt.BorderLayout
    (add!* [layout target widget constraint]
      (add-widget target widget (border-layout-dirs constraint)))
    (get-constraint [layout container widget]
      (.getConstraints layout widget))

  net.miginfocom.swing.MigLayout
    (add!* [layout target widget constraint]
      (add-widget target widget))
    (get-constraint [layout container widget] (.getComponentConstraints layout widget)))


(defn- add!-impl 
  [container subject & more]
  (let [container (to-widget container)
        [widget constraint] (if (vector? subject) subject [subject nil])
        layout (.getLayout container)]
    (add!* layout container widget constraint)
    (when more
      (apply add!-impl container more))
    container))

(defn add! [container subject & more]
  "Add one or more widgets to a widget container. The container and each widget
  argument are passed through (to-widget) as usual. Each widget can be a single
  widget, or a widget/constraint pair with a layout-specific constraint.

  The container is properly revalidated and repainted after removal.

  Examples:

    ; Add a label and a checkbox to a panel
    (add! (vertical-panel) \"Hello\" (button ...))

    ; Add a label and a checkbox to a border panel with layout constraints
    (add! (border-panel) [\"Hello\" :north] [(button ...) :center])

  Returns the target container *after* it's been passed through (to-widget).
  "
  (handle-structure-change (apply add!-impl container subject more)))

(defn- remove!-impl
  [container subject & more]
  (let [container (to-widget container)]
    (.remove (to-widget container) (to-widget subject))
    (when more
      (apply remove!-impl container more))
    container))

(defn remove!
  "Remove one or more widgets from a container. container and each widget
  are passed through (to-widget) as usual, but no new widgets are created.

  The container is properly revalidated and repainted after removal.

  Examples:

    (def lbl (label \"HI\"))
    (def p (border-panel :north lbl))
    (remove! p lbl)

  Returns the target container *after* it's been passed through (to-widget).
  "
  [container subject & more]
  (handle-structure-change (apply remove!-impl container subject more)))

(defn- index-of-component
  [container widget]
  (loop [comps (.getComponents container) idx 0]
    (cond
      (not comps)              nil
      (= widget (first comps)) idx
      :else (recur (next comps) (inc idx)))))

(defn- replace!-impl
  [container old-widget new-widget]
  (let [container  (to-widget container)
        old-widget (to-widget old-widget)
        idx        (index-of-component container old-widget)]
    (when idx
      (let [constraint (get-constraint (.getLayout container) container old-widget)]
        (doto container
          (.remove idx)
          (.add    (to-widget new-widget true) constraint))))
    container))
  
(defn replace!
  "Replace old-widget with new-widget from container. container and each widget
  are passed through (to-widget) as usual. Note that the layout constraints of 
  old-widget are retained for the new widget. This is different from the behavior
  you'd get with just remove/add in Swing.

  The container is properly revalidated and repainted after replacement.

  Examples:

    ; Replace a label with a new label.
    (def lbl (label \"HI\"))
    (def p (border-panel :north lbl))
    (replace! p lbl \"Goodbye\")

  Returns the target container *after* it's been passed through (to-widget).
  "
  [container old-widget new-widget]
  (handle-structure-change (replace!-impl container old-widget new-widget)))

