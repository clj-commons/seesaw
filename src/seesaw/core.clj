;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc
"Core functions and macros for Seesaw. Although there are many more
  Seesaw namespaces, usually what you want is in here. Most functions
  in other namespaces have a core wrapper which adds additional
  capability or makes them easier to use."
      :author "Dave Ray"}
  seesaw.core
  (:use [seesaw.util :only [illegal-argument to-seq check-args
                            constant-map resource resource-key?
                            to-dimension to-insets to-url try-cast
                            cond-doto to-mnemonic-keycode]]
        [seesaw.config :only [Configurable config* config!*]]
        [seesaw.options :only [ignore-option default-option bean-option
                               resource-option around-option
                               apply-options
                               option-map option-provider
                               get-option-value]]
        [seesaw.widget-options :only [widget-option-provider]]
        [seesaw.meta :only [get-meta put-meta!]]
        [seesaw.to-widget :only [ToWidget to-widget*]]
        [seesaw.make-widget :only [make-widget*]])
  (:require clojure.java.io
            clojure.set
            [seesaw color font border invoke timer selection value
             event selector icon action cells table graphics cursor scroll dnd]
            [seesaw.layout :as layout])
  (:import [javax.swing
             SwingConstants UIManager ScrollPaneConstants DropMode
             BoxLayout
             JDialog JFrame JComponent Box JPanel JScrollPane JSplitPane JToolBar JTabbedPane
             JLabel JTextField JTextArea JTextPane
             AbstractButton JButton ButtonGroup
             JOptionPane]
           [javax.swing.text JTextComponent StyleConstants]
           [java.awt Component FlowLayout BorderLayout GridLayout
              GridBagLayout GridBagConstraints
              Dimension]))

(declare to-widget)
(declare popup-option-handler)

; seesaw.invoke aliases. These were originally aliases with def, but this caused
; the weird behavior in issue #73.

(defmacro invoke-now
  "Alias for seesaw.invoke/invoke-now"
  [& args]
  `(seesaw.invoke/invoke-now ~@args))

(defmacro invoke-later
 "Alias for seesaw.invoke/invoke-later"
  [& args]
  `(seesaw.invoke/invoke-later ~@args))

(defmacro invoke-soon
 "Alias for seesaw.invoke/invoke-soon"
  [& args]
  `(seesaw.invoke/invoke-soon ~@args))

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
  (when-not (javax.swing.SwingUtilities/isEventDispatchThread)
    (throw (IllegalStateException.
             (str "Expected UI thread, but got '"
                  (.. (Thread/currentThread) getName)
                  "' : "
                  message)))))

; TODO make a macro for this. There's one in contrib I think, but I don't trust contrib.

; alias timer/timer for convenience
(def ^{:doc (str "Alias of seesaw.timer/timer:\n" (:doc (meta #'seesaw.timer/timer)))} timer seesaw.timer/timer)

; alias event/listen for convenience
(def ^{:doc (str "Alias of seesaw.event/listen:\n" (:doc (meta #'seesaw.event/listen)))} listen seesaw.event/listen)

; alias action/action for convenience
(def ^{:doc (str "Alias of seesaw.action/action:\n" (:doc (meta #'seesaw.action/action)))} action seesaw.action/action)


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
    (seesaw.core/selection!)
    (seesaw.selection/selection)
  "
  ([target] (selection target {}))
  ([target options] (seesaw.selection/selection (to-selectable target) options)))

(defn selection!
  "Sets the selection on a widget. target is passed through (to-widget)
  so event objects can also be used. The arguments are the same as
  (selection). By default, new-selection is a single new selection value.
  If new-selection is nil, the selection is cleared.

  options is an option map which supports the following flags:

    multi? - if true new-expression is a list of values to selection,
      the same as the list returned by (selection).

  Always returns target.

  See:
    (seesaw.core/selection)
    (seesaw.selection/selection!)
  "
  ([target new-selection] (selection! target {} new-selection))
  ([target opts new-selection] (seesaw.selection/selection! (to-selectable target) opts new-selection)))

(def icon seesaw.icon/icon)
(def ^{:private true} make-icon icon)

;*******************************************************************************
; Widget coercion prototcol

(defn ^java.awt.Component make-widget
  "Try to create a new widget based on the following rules:

    nil -> nil
    java.awt.Component -> return argument unchanged (like to-widget)
    java.util.EventObject -> return the event source (like to-widget)

    java.awt.Dimension -> return Box/createRigidArea
    java.swing.Action    -> return a button using the action
    :separator -> create a horizontal JSeparator
    :fill-h -> Box/createHorizontalGlue
    :fill-v -> Box/createVerticalGlue
    [:fill-h n] -> Box/createHorizontalStrut with width n
    [:fill-v n] -> Box/createVerticalStrut with height n
    [width :by height] -> create rigid area with given dimensions
    java.net.URL -> a label with the image located at the url
    Anything else -> a label with the text from passing the object through str
  "
  ([v] (when v (make-widget* v))))

(defn ^java.awt.Component to-widget
  "Try to convert the input argument to a widget based on the following rules:

    nil -> nil
    java.awt.Component -> return argument unchanged
    java.util.EventObject -> return the event source

  See:
    (seeseaw.to-widget)
  "
  ([v] (when v (to-widget* v))))

;*******************************************************************************
; Widget construction stuff


(defmacro construct
  "*experimental. subject to change.*

  A macro that returns a proxied instance of the given class. This is
  used by Seesaw to construct widgets that can be fiddled with later,
  e.g. installing a paint handler, etc."
  ([factory-class & opts]
    `(proxy [~factory-class seesaw.selector.Tag] [~@opts]
       (tag_name [] (.getSimpleName ~factory-class)))))


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

(defn all-frames
  "Returns a sequence of all of the frames (includes java.awt.Frame) known by the JVM.

  This function is really only useful for debugging and repl development, namely:

    ; Clear out all frames
    (dispose! (all-frames))

  Otherwise, it is highly unreliable. Frames will hang around after disposal, pile up
  and generally cause trouble.

  You've been warned."
  []
  (seq (java.awt.Frame/getFrames)))

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
  (doseq [^java.awt.Component target (map to-widget (to-seq targets))]
    (.repaint target))
  targets)

(defn request-focus!
  "Request focus for the given widget-able thing. This will try to give
  keyboard focus to the given widget. Returns its input.

  The widget must be :focusable? for this to succeed.

  Example:
    (request-focus! my-widget)

    ; Move focus on click
    (listen my-widget :focus-gained request-focus!)

  See:
    http://docs.oracle.com/javase/6/docs/api/javax/swing/JComponent.html#requestFocusInWindow()
  "
  [target]
  (let [^java.awt.Component w (to-widget target)]
    (.requestFocusInWindow w))
  target)

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
          layout/handle-structure-change)
        this))
    (move-to-back! [this]
      (let [parent (.getParent this)
            n      (.getComponentCount parent)]
        (doto parent
          (.setComponentZOrder this (dec n))
          layout/handle-structure-change)
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
                    (instance? java.awt.Point loc) (let [^java.awt.Point loc loc] [(.x loc) (.y loc)])
                    (instance? java.awt.Rectangle loc) (let [^java.awt.Rectangle loc loc] [(.x loc) (.y loc)])
                    (= how :to) (replace {:* nil} loc)
                    :else loc)]
        (case how
          :to      (move-to! target x y)
          :by      (move-by! target x y)))
    :to-front
      (move-to-front! target)
    :to-back
      (move-to-back! target)))

(defn width
  "Returns the width of the given widget in pixels"
  [w]
  (.getWidth (to-widget w)))

(defn height
  "Returns the height of the given widget in pixels"
  [w]
  (.getHeight (to-widget w)))


(defn id-of
  "Returns the id of the given widget if the :id property was specified at
   creation. The widget parameter is passed through (to-widget) first so
   events and other objects can also be used. The id is always returned as
   a string, even it if was originally given as a keyword.

  Returns the id as a keyword, or nil.

  See:
    (seesaw.core/select).
  "
  [w]
  (seesaw.selector/id-of (to-widget w)))

(def ^{:doc "Deprecated. See (seesaw.core/id-of)"} id-for id-of)

(defn user-data
  "Convenience function to retrieve the value of the :user-data option
  passed to the widget at construction. The widget parameter is passed
  through (to-widget) first so events and other objects can also be
  used.

  Examples:

    (user-data (label :text \"HI!\" :user-data 99))
    ;=> 99
  "
  [w]
  (config* w :user-data))

(def ^{:private true} h-alignment-table
  (constant-map SwingConstants :left :right :leading :trailing :center ))

(def ^{:private true} v-alignment-table
  (constant-map SwingConstants :top :center :bottom))

(let [table (constant-map SwingConstants :horizontal :vertical)]
  (defn- orientation-table [v]
    (or (table v)
        (illegal-argument
          ":orientation must be either :horizontal or :vertical. Got %s instead." v))))

(defn- bounds-option-handler [^java.awt.Component target v]
  (cond
    ; TODO to-rect protocol?
    (= :preferred v)
      (bounds-option-handler target (.getPreferredSize target))
    (instance? java.awt.Rectangle v) (.setBounds target v)
    (instance? java.awt.Dimension v)
      (let [loc (.getLocation target)
            v   ^java.awt.Dimension v]
        (.setBounds target (.x loc) (.y loc) (.width v) (.height v)))
    :else
      (let [old       (.getBounds target)
            [x y w h] (replace {:* nil} v)]
        (.setBounds target
            (or x (.x old))     (or y (.y old))
            (or w (.width old)) (or h (.height old))))))


;*******************************************************************************
; Widget configuration stuff

(def ^{:doc (str "Alias of seesaw.config/config:\n" (:doc (meta #'seesaw.config/config)))} config seesaw.config/config)

(def ^{:doc (str "Alias of seesaw.config/config!:\n" (:doc (meta #'seesaw.config/config!)))} config! seesaw.config/config!)

;*******************************************************************************
; Default options

; We define a few protocols for various setters that existing on multiple Swing
; types, but don't have a common interface. This lets us avoid reflection.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; get/setText is a common method on many types, but not in any common interface :(

(defprotocol ConfigIcon
  "Protocol to hook into :icon option"
  (set-icon* [this v])
  (get-icon* [this]))

(extend-protocol ConfigIcon
  ; most things don't have icons...
  java.awt.Component
    (set-icon* [this v]
      (illegal-argument "%s does not support the :icon option" (class this)))
    (get-icon* [this]
      (illegal-argument "%s does not support the :icon option" (class this)))

  javax.swing.JLabel
    (set-icon* [this v] (.setIcon this (make-icon v)))
    (get-icon* [this] (.getIcon this))

  javax.swing.AbstractButton
    (set-icon* [this v] (.setIcon this (make-icon v)))
    (get-icon* [this] (.getIcon this)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; get/setText is a common method on many types, but not in any common interface :(

(defprotocol ConfigText
  "Protocol to hook into :text option"
  (set-text* [this v])
  (get-text* [this]))

(extend-protocol ConfigText
  Object
    (set-text* [this v] (set-text* (to-widget this) v))
    (get-text* [this] (get-text* (to-widget this)))
  java.awt.Component
    (set-text* [this v]
      (illegal-argument "%s does not support (seesaw.core/text!)" (class this)))
    (get-text* [this]
      (illegal-argument "%s does not support (seesaw.core/text)" (class this)))
  javax.swing.JLabel
    (set-text* [this v] (.setText this v))
    (get-text* [this] (.getText this))
  javax.swing.AbstractButton
    (set-text* [this v] (.setText this v))
    (get-text* [this] (.getText this))
  javax.swing.text.AbstractDocument
    (set-text* [this v] (.replace this 0 (.getLength this) v nil))
    (get-text* [this] (.getText this 0 (.getLength this)))
  javax.swing.event.DocumentEvent
    (set-text* [this v] (set-text* (.getDocument this) v))
    (get-text* [this] (get-text* (.getDocument this)))
  javax.swing.text.JTextComponent
    (set-text* [this v] (.setText this v))
    (get-text* [this] (.getText this))
  javax.swing.JComboBox
    (set-text* [this v] )
    (get-text* [this]
      (if-let [i (selection this)]
        (str i))))

(defn- convert-text-value [v]
  (cond
    (nil? v)          v
    (string? v)       v
    (number? v)       (str v)
    (resource-key? v) (resource v)
    (satisfies? clojure.java.io/IOFactory v) (slurp v)
    ; TODO This line is unreachable because the IOFactory protocol is
    ; extended to Object, i.e. satisfies? above will *always* return
    ; true :(
    :else (str v)))

(defn- set-text
  "Internal use only"
  [this v]
  (set-text* this (convert-text-value v)))

(defn- get-text
  "Internal use only"
  [this]
  (get-text* this))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; setAction is a common method on many types, but not in any common interface :(

(defprotocol ConfigAction
  "Protocol to hook into :action option"
  (set-action* [this v])
  (get-action* [this]))

(extend-protocol ConfigAction
  javax.swing.AbstractButton
    (get-action* [this] (.getAction this))
    (set-action* [this v] (.setAction this v))
  javax.swing.JTextField
    (get-action* [this] (.getAction this))
    (set-action* [this v] (.setAction this v))
  javax.swing.JComboBox
    (get-action* [this] (.getAction this))
    (set-action* [this v] (.setAction this v)))

(def ^{:doc "Default handler for the :action option. Internal use."}
  action-option (default-option :action set-action* get-action* "See (seesaw.core/action)"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; set/getModel is a common method on many types, but not in any common interface :(

(defprotocol ConfigModel
  "Protocol to hook into :model option"
  (get-model* [this])
  (set-model* [this m]))

(extend-protocol ConfigModel
  javax.swing.text.JTextComponent
    (get-model* [this] (.getDocument this))
    (set-model* [this v] (.setDocument this v)))

(defmacro ^{:private true} config-model-impl [& classes]
  `(extend-protocol ConfigModel
   ~@(mapcat
      (fn [c]
        `(~c (~'get-model* [this#] (. this# ~'getModel))
             (~'set-model* [this# v#] (. this# ~'setModel v#))))
     classes)))

(config-model-impl
  javax.swing.AbstractButton
  javax.swing.JComboBox
  javax.swing.JList
  javax.swing.JTable
  javax.swing.JTree
  javax.swing.JProgressBar
  javax.swing.JSlider
  javax.swing.JScrollBar
  javax.swing.JSpinner)

(def ^{:doc "Default handler for the :model option. Delegates to the ConfigModel protocol"}
  model-option (default-option :model set-model* get-model*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; dragEnabled is a common method on many types, but not in any common interface :(
(defprotocol ^{:private true} ConfigDragEnabled
  (get-drag-enabled [this])
  (set-drag-enabled [this v]))

; Do-nothing impls for everybody
(extend-protocol ConfigDragEnabled
  javax.swing.JComponent (get-drag-enabled [this] false) (set-drag-enabled [this v])
  javax.swing.JWindow    (get-drag-enabled [this] false) (set-drag-enabled [this v]))

(defmacro ^{:private true} config-drag-enabled-impl [& classes]
  `(extend-protocol ConfigDragEnabled
   ~@(mapcat
      (fn [c]
        `(~c (~'get-drag-enabled [this#] (. this# ~'getDragEnabled))
             (~'set-drag-enabled [this# v#] (. this# ~'setDragEnabled (boolean v#)))))
     classes)))

(config-drag-enabled-impl
  javax.swing.text.JTextComponent
  javax.swing.JColorChooser
  javax.swing.JFileChooser
  javax.swing.JTable
  javax.swing.JList
  javax.swing.JTree)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; drop-mode support constants

(def ^{:private true} drop-mode-to-keyword {
  DropMode/INSERT            :insert
  DropMode/INSERT_COLS       :insert-cols
  DropMode/INSERT_ROWS       :insert-rows
  DropMode/ON                :on
  DropMode/ON_OR_INSERT      :on-or-insert
  DropMode/ON_OR_INSERT_COLS :on-or-insert-cols
  DropMode/ON_OR_INSERT_ROWS :on-or-insert-rows
  DropMode/USE_SELECTION     :use-selection
})

(def ^{:private true} keyword-to-drop-mode (clojure.set/map-invert drop-mode-to-keyword))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; layout-orientation support constants

(defprotocol LayoutOrientationConfig
  "Hook protocol for :layout-orientation option"
  (set-layout-orientation* [this v])
  (get-layout-orientation* [this]))

(extend-protocol LayoutOrientationConfig
  javax.swing.JList
    (set-layout-orientation* [this v] (.setLayoutOrientation this v))
    (get-layout-orientation* [this] (.getLayoutOrientation this)))

(defn- layout-orientation-option [table]
  (let [rtable (clojure.set/map-invert table)]
    (default-option
      :layout-orientation
      (fn [target v]
        (if-let [v (table v)]
          (set-layout-orientation* target v)
          (illegal-argument "Unknown layout-orientation. Must be one of %s" (keys table))))
      (fn [target] (rtable (get-layout-orientation* target)))
      (keys table))))

(def ^{:private true} list-layout-orientation-table {
  :vertical        javax.swing.JList/VERTICAL
  :horizontal-wrap javax.swing.JList/HORIZONTAL_WRAP
  :vertical-wrap   javax.swing.JList/VERTICAL_WRAP
})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol SelectionModeConfig
  "Hook protocol for :selection-mode option"
  (set-selection-mode* [this v])
  (get-selection-mode* [this]))

(extend-protocol SelectionModeConfig
  javax.swing.tree.TreeSelectionModel
    (set-selection-mode* [this v] (.setSelectionMode this v))
    (get-selection-mode* [this] (.getSelectionMode this))

  javax.swing.JTree
    (set-selection-mode* [this v] (set-selection-mode* (.getSelectionModel this) v))
    (get-selection-mode* [this] (get-selection-mode* (.getSelectionModel this)))

  javax.swing.ListSelectionModel
    (set-selection-mode* [this v] (.setSelectionMode this v))
    (get-selection-mode* [this]   (.getSelectionMode this))

  javax.swing.JTable
    (set-selection-mode* [this v] (set-selection-mode* (.getSelectionModel this) v))
    (get-selection-mode* [this] (get-selection-mode* (.getSelectionModel this)))
  javax.swing.JList
    (set-selection-mode* [this v] (.setSelectionMode this v))
    (get-selection-mode* [this] (.getSelectionMode this)))

(defn- selection-mode-option [table]
  (let [rtable (clojure.set/map-invert table)]
    (default-option
      :selection-mode
      (fn [target v]
        (if-let [v (table v)]
          (set-selection-mode* target v)
          (illegal-argument "Unknown selection-mode. Must be one of %s" (keys table))))
      (fn [target] (rtable (get-selection-mode* target)))
      (keys table))))

(def ^{:private true} list-selection-mode-table {
  :single          javax.swing.ListSelectionModel/SINGLE_SELECTION
  :single-interval javax.swing.ListSelectionModel/SINGLE_INTERVAL_SELECTION
  :multi-interval  javax.swing.ListSelectionModel/MULTIPLE_INTERVAL_SELECTION
})

(def ^ {:private true} tree-selection-mode-table {
  :single        javax.swing.tree.TreeSelectionModel/SINGLE_TREE_SELECTION
  :contiguous    javax.swing.tree.TreeSelectionModel/CONTIGUOUS_TREE_SELECTION
  :discontiguous javax.swing.tree.TreeSelectionModel/DISCONTIGUOUS_TREE_SELECTION
})

(declare paint-option-handler)

(def ^{:private true} color-examples [:aliceblue "\"#f00\"" "\"#FF0000\"" '(seesaw.color/color 255 0 0 0 224)])
(def ^{:private true} boolean-examples 'boolean)
(def ^{:private true} dimension-examples [[640 :by 480] 'java.awt.Dimension])

(def base-resource-options [:text :foreground :background :font :icon :tip])

(def default-options
  (option-map
    (bean-option :layout JComponent nil nil "A layout manager.")
    (default-option :listen #(apply seesaw.event/listen %1 %2) nil ["vector of args for (seesaw.core/listen)"])

    (default-option :id seesaw.selector/id-of! seesaw.selector/id-of ["A keyword id for the widget"])
    (default-option :class seesaw.selector/class-of! seesaw.selector/class-of [:class-name, #{:multiple, :class-names}])

    (default-option
      :user-data
      (fn [c v] (put-meta! c ::user-data v))
      (fn [c]   (get-meta c ::user-data))
      ["Anything."
       "Associate arbitrary user-data with a widget."
       "See (seesaw.core/user-data)"])

    (bean-option :opaque? JComponent boolean nil boolean-examples)
    (bean-option :enabled? java.awt.Component boolean nil boolean-examples)
    (bean-option :focusable? java.awt.Component boolean nil boolean-examples)
    (default-option :background
                    #(do
                      (.setBackground ^JComponent %1 (seesaw.color/to-color %2))
                      (.setOpaque ^JComponent %1 true))
                    #(.getBackground ^JComponent %1)
                    color-examples)
    (bean-option :foreground JComponent seesaw.color/to-color nil color-examples)
    (bean-option :border JComponent seesaw.border/to-border nil [5, "\"Border Title\"", [5 "Compound" 10], "See (seesaw.border/*)"])
    (bean-option :font JComponent seesaw.font/to-font nil ["ARIAL-BOLD-18", :monospaced :serif :sans-serif "See (seesaw.font/font)"])
    (bean-option [:tip :tool-tip-text] JComponent str nil ["A tooltip string"])
    (bean-option :cursor java.awt.Component #(apply seesaw.cursor/cursor (to-seq %)) nil ["See (seesaw.cursor/cursor)"])
    (bean-option :visible? java.awt.Component boolean nil boolean-examples)
    (bean-option :preferred-size JComponent to-dimension nil dimension-examples)
    (bean-option :minimum-size JComponent to-dimension nil dimension-examples)
    (bean-option :maximum-size JComponent to-dimension nil dimension-examples)
    (default-option :size
                      #(let [d (to-dimension %2)]
                        (doto ^JComponent %1
                          (.setPreferredSize d)
                          (.setMinimumSize d)
                          (.setMaximumSize d)))
                      #(.getSize ^JComponent %1)
      dimension-examples)

    (default-option :location
      #(move! %1 :to %2)
      #(.getLocation ^java.awt.Component %1)
      ["See (seesaw.core/move! :to)"])

    (default-option :location-on-screen
      nil
      #(.getLocationOnScreen ^java.awt.Component %1)
      ["java.awt.Point location in global screen coords"])

    (default-option :bounds
      bounds-option-handler
      #(.getBounds ^java.awt.Component %1)
      [:preferred '[x y w h] "Use :* to leave component unchanged:"
       '[x :* :* h]])
    (default-option :popup
      #(popup-option-handler %1 %2)
      nil
      ['javax.swing.JPopupMenu
       "(fn [e]) that returns a seq of menu items"
       "See (seesaw.core/popup)"])
    (default-option :paint #(paint-option-handler %1 %2) nil ["See (seesaw.core/canvas)"])

    ; TODO I'd like to push these down but cells.clj uses them on non-attached
    ; widgets.
    (default-option :icon set-icon* get-icon* ["See (seesaw.icon/icon)"])
    (default-option :text set-text get-text ["A string" "Anything accepted by (clojure.core/slurp)"])

    (default-option :drag-enabled? set-drag-enabled get-drag-enabled boolean-examples)
    (bean-option :transfer-handler JComponent
                 seesaw.dnd/to-transfer-handler
                 identity
                 "See (seesaw.dnd/to-transfer-handler)")))

(widget-option-provider
  javax.swing.JPanel
  default-options
  layout/nil-layout-options)

(extend-protocol Configurable
  java.util.EventObject
    (config* [target name] (config* (to-widget target) name))
    (config!* [target args] (config!* (to-widget target) args))

  java.awt.Component
    (config* [target name] (get-option-value target name))
    (config!* [target args] (apply-options target args))

  javax.swing.JComponent
    (config* [target name] (get-option-value target name))
    (config!* [target args] (apply-options target args))

  javax.swing.Action
    (config* [target name] (get-option-value target name))
    (config!* [target args] (apply-options target args))

  java.awt.Window
    (config* [target name] (get-option-value target name))
    (config!* [target args] (apply-options target args)))

;*******************************************************************************
; ToDocument

; TODO ToDocument protocol
(defn ^javax.swing.text.AbstractDocument to-document
  [v]
  (let [w (to-widget v)]
    (cond
      (instance? javax.swing.text.Document v)       v
      (instance? javax.swing.event.DocumentEvent v) (.getDocument ^javax.swing.event.DocumentEvent v)
      (instance? JTextComponent w)                  (.getDocument ^JTextComponent w))))

;*******************************************************************************
; Abstract Panel
(defn abstract-panel
  ([panel layout opts]
    (doto panel
      (.setLayout (if (fn? layout) (layout panel) layout))
      (apply-options opts)))
  ([layout opts] (abstract-panel (construct JPanel) layout opts)))

;*******************************************************************************
; Null Layout

(defn xyz-panel
  "Creates a JPanel on which widgets can be positioned arbitrarily by client
  code. No layout manager is installed.

  Initial widget positions can be given with their :bounds property. After
  construction they can be moved with the (seesaw.core/move!) function.

  Examples:

    ; Create a panel with a label positions at (10, 10) with width 200 and height 40.
    (xyz-panel :items [(label :text \"The Black Lodge\" :bounds [10 10 200 40])])

    ; Move a widget up 50 pixels and right 25 pixels
    (move! my-label :by [25 -50])

  Notes:

  See:
    (seesaw.core/move!)
  "
  [& opts]
  (abstract-panel nil opts))

;*******************************************************************************
; Border Layout


(def border-panel-options default-options)

(defn border-panel
  "Create a panel with a border layout. In addition to the usual options,
  supports:

    :north  widget for north position (passed through make-widget)
    :south  widget for south position (passed through make-widget)
    :east   widget for east position (passed through make-widget)
    :west   widget for west position (passed through make-widget)
    :center widget for center position (passed through make-widget)

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
  (abstract-panel (BorderLayout.) opts))

;*******************************************************************************
; Card

(def card-panel-options default-options)

(defn card-panel
  "Create a panel with a card layout. Options:

    :items A list of pairs with format [widget, identifier]
           where identifier is a string or keyword.

  See:

    (seesaw.core/show-card!)
    http://download.oracle.com/javase/6/docs/api/java/awt/CardLayout.html
  "
  [& opts]
  (abstract-panel (java.awt.CardLayout.) opts))

; TODO move to layout.clj
(defn show-card!
  "Show a particular card in a card layout. id can be a string or keyword.
   panel is returned.

  See:

    (seesaw.core/card-panel)
    http://download.oracle.com/javase/6/docs/api/java/awt/CardLayout.html
  "
  [^java.awt.Container panel id]
  (.show ^java.awt.CardLayout (.getLayout panel) panel (name id))
  panel)

;*******************************************************************************
; Flow

(def flow-panel-options default-options)

(defn flow-panel
  "Create a panel with a flow layout. Options:

    :items  List of widgets (passed through make-widget)
    :hgap   horizontal gap between widgets
    :vgap   vertical gap between widgets
    :align  :left, :right, :leading, :trailing, :center
    :align-on-baseline?

  See http://download.oracle.com/javase/6/docs/api/java/awt/FlowLayout.html
  "
  [& opts]
  (abstract-panel (FlowLayout.) opts))

;*******************************************************************************
; Boxes

(def box-panel-options default-options)

(defn box-panel
  [dir & opts]
  (abstract-panel (layout/box-layout dir) opts))

(def horizontal-panel-options box-panel-options)

(defn horizontal-panel
  "Create a panel where widgets are arranged horizontally. Options:

    :items List of widgets (passed through make-widget)

  See http://download.oracle.com/javase/6/docs/api/javax/swing/BoxLayout.html
  "
  [& opts] (apply box-panel :horizontal opts))

(def vertical-panel-options box-panel-options)

(defn vertical-panel
  "Create a panel where widgets are arranged vertically Options:

    :items List of widgets (passed through make-widget)

  See http://download.oracle.com/javase/6/docs/api/javax/swing/BoxLayout.html
  "
  [& opts] (apply box-panel :vertical opts))

;*******************************************************************************
; Grid

(def grid-panel-options default-options)

(defn grid-panel
  "Create a panel where widgets are arranged horizontally. Options:

    :rows    Number of rows, defaults to 0, i.e. unspecified.
    :columns Number of columns.
    :items   List of widgets (passed through make-widget)
    :hgap    horizontal gap between widgets
    :vgap    vertical gap between widgets

  Note that it's usually sufficient to just give :columns and ignore :rows.

  See http://download.oracle.com/javase/6/docs/api/java/awt/GridLayout.html
  "
  [& {:keys [rows columns]
      :as opts}]
  (abstract-panel (layout/grid-layout rows columns) opts))

;*******************************************************************************
; Form aka GridBagLayout


(def form-panel-options default-options)

(defn form-panel
  "*Don't use this. GridBagLaout is an abomination* I suggest using Seesaw's
  MigLayout (seesaw.mig) or JGoogies Forms (seesaw.forms) support instead.

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
  (abstract-panel (GridBagLayout.) opts))

(def grid-bag-panel form-panel)

;*******************************************************************************
; Labels

(def label-options
  (merge
    default-options
    (option-map
      (resource-option :resource base-resource-options)
      (bean-option [:halign :horizontal-alignment] javax.swing.JLabel h-alignment-table nil (keys h-alignment-table))
      (bean-option [:valign :vertical-alignment] javax.swing.JLabel v-alignment-table nil (keys v-alignment-table))
      (bean-option [:h-text-position :horizontal-text-position] javax.swing.JLabel h-alignment-table nil (keys h-alignment-table))
      (bean-option [:v-text-position :vertical-text-position] javax.swing.JLabel v-alignment-table nil (keys v-alignment-table)))))

(widget-option-provider javax.swing.JLabel label-options)

(defn label
  "Create a label. Supports all default properties. Can take two forms:

      (label \"My Label\")  ; Single text argument for the label

  or with full options:

      (label :id :my-label :text \"My Label\" ...)

  Additional options:

    :h-text-position Horizontal text position, :left, :right, :center, etc.
    :v-text-position Horizontal text position, :top, :center, :bottom, etc.
    :resource        Namespace-qualified keyword which is a resource prefix for the
                     labels properties

  Resources and i18n:

    A label's base properties can be set from a resource prefix, i.e. a namespace-
    qualified keyword that refers to a resource bundle loadable by j18n.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JLabel.html
  "
  [& args]
  (case (count args)
    0 (label :text "")
    1 (label :text (first args))
    (apply-options (construct JLabel) args)))


;*******************************************************************************
; Buttons
(extend-protocol Configurable
  javax.swing.ButtonGroup
    (config* [target name] (get-option-value target name))
    (config!* [target args] (apply-options target args)))

(def button-group-options
  (option-map
    (default-option :buttons
      #(doseq [b %2] (.add ^javax.swing.ButtonGroup %1 b))
      #(enumeration-seq (.getElements ^javax.swing.ButtonGroup %1))
      ["A seq of buttons in the group"])))

(option-provider javax.swing.ButtonGroup button-group-options)

(defn button-group
  "Creates a button group, i.e. a group of mutually exclusive toggle buttons,
  radio buttons, toggle-able menus, etc. Takes the following options:

    :buttons A sequence of buttons to include in the group. They are *not*
             passed through (make-widget), i.e. they must be button or menu
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

    ; Listen for selection changes. Note that the selection MAY BE NIL!
    ; Also note that the event that comes through is from the selected radio button
    ; *not the button-group itself* since the button-group is a somewhat artificial
    ; construct. So, you'll have to ask for (selection bg) instead of (selection e) : (
    (listen bg :selection
      (fn [e]
        (if-let [s (selection bg)]
          (println \"Selected \" (text s)))))

  Returns an instance of javax.swing.ButtonGroup

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/ButtonGroup.html
  "
  [& opts]
  (apply-options (ButtonGroup.) opts))

(def button-options
  (merge
    default-options
    (option-map
      model-option
      action-option
      (resource-option :resource base-resource-options)
      (bean-option [:halign :horizontal-alignment] javax.swing.AbstractButton h-alignment-table nil (keys h-alignment-table))
      (bean-option [:valign :vertical-alignment] javax.swing.AbstractButton v-alignment-table nil (keys v-alignment-table))
      (bean-option :selected? javax.swing.AbstractButton boolean nil boolean-examples)
      (bean-option :margin javax.swing.AbstractButton to-insets)

      (default-option :group #(.add ^javax.swing.ButtonGroup %2 %1) nil ["A button group"])
      (bean-option :mnemonic javax.swing.AbstractButton to-mnemonic-keycode nil ["See (seesaw.util/to-mnemonic-keycode)"]))))

(widget-option-provider javax.swing.AbstractButton button-options)

(defn button
  "Construct a generic button. In addition to default widget options, supports
  the following:

      :halign    Horizontal alignment. One of :left, :right, :leading, :trailing,
                 :center
      :valign    Vertical alignment. One of :top, :center, :bottom
      :selected? Whether the button is initially selected. Mostly for checked
                 and radio buttons/menu-items.
      :margin    The button margins as insets. See (seesaw.util/to-insets)
      :group     A button-group that the button should be added to.
      :resource  A resource prefix (see below).
      :mnemonic  The mnemonic for the button, either a character or a keycode.
                  Usually allows the user to activate the button with alt-mnemonic.
                  See (seesaw.util/to-mnemonic-keycode).

  Resources and i18n:

    A button's base properties can be set from a resource prefix, i.e. a namespace-
    qualified keyword that refers to a resource bundle loadable by j18n.

  Examples:

    ; Create a button with text \"Next\" with alt-N mnemonic shortcut that shows
    ; an alert when clicked.
    (button :text \"Next\"
            :mnemonic \\N
            :listen [:action #(alert % \"NEXT!\")])

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JButton.html
    (seesaw.core/button-group)"
  [& args]
  (apply-options (construct javax.swing.JButton) args))

(def toggle-options button-options)

(defn toggle
  "Same as (seesaw.core/button), but creates a toggle button. Use :selected? option
  to set initial state.

  See:
    (seesaw.core/button)"
  [& args]
  (apply-options (construct javax.swing.JToggleButton) args))

(def checkbox-options button-options)

(defn checkbox
  "Same as (seesaw.core/button), but creates a checkbox. Use :selected? option
  to set initial state.

  See:
    (seesaw.core/button)"
  [& args]
  (apply-options (construct javax.swing.JCheckBox) args))

(def radio-options button-options)

(defn radio
  "Same as (seesaw.core/button), but creates a radio button. Use :selected? option
  to set initial state.

  See:
    (seesaw.core/button)"
  [& args]
  (apply-options (construct javax.swing.JRadioButton) args))

;*******************************************************************************
; Text widgets
(def text-options
  (merge
    default-options
    (option-map
      model-option
      action-option
      (resource-option :resource (concat base-resource-options
                                        [:caret-color :disabled-text-color :selected-text-color :selection-color]))
      (bean-option :editable? javax.swing.text.JTextComponent boolean)
      (bean-option :margin javax.swing.text.JTextComponent to-insets)
      (bean-option :caret-color javax.swing.text.JTextComponent seesaw.color/to-color nil color-examples)
      (bean-option :caret-position javax.swing.text.JTextComponent)
      (bean-option :disabled-text-color javax.swing.text.JTextComponent seesaw.color/to-color nil color-examples)
      (bean-option :selected-text-color javax.swing.text.JTextComponent seesaw.color/to-color nil color-examples)
      (bean-option :selection-color javax.swing.text.JTextComponent seesaw.color/to-color nil color-examples)
      (bean-option :drop-mode javax.swing.text.JTextComponent keyword-to-drop-mode drop-mode-to-keyword (keys keyword-to-drop-mode)))))

(widget-option-provider javax.swing.text.JTextComponent text-options)

(def text-field-options
  (merge
    text-options
    (option-map
      (bean-option [:halign :horizontal-alignment] javax.swing.JTextField h-alignment-table nil (keys h-alignment-table))
      (bean-option :columns javax.swing.JTextField))))

(widget-option-provider javax.swing.JTextField text-field-options)

(def text-area-options
  (merge
    text-options
    (option-map
      (bean-option :columns javax.swing.JTextArea)
      (bean-option :rows javax.swing.JTextArea)
      (default-option :wrap-lines?
        #(doto ^javax.swing.JTextArea %1
           (.setLineWrap (boolean %2))
           (.setWrapStyleWord (boolean %2)))
        #(.getLineWrap ^javax.swing.JTextArea %1))
      (bean-option :tab-size javax.swing.JTextArea))))

(widget-option-provider javax.swing.JTextArea text-area-options)

(defn text
  "Create a text field or area. Given a single argument, creates a JTextField
  using the argument as the initial text value. Otherwise, supports the
  following additional properties:

    :text         Initial text content
    :multi-line?  If true, a JTextArea is created (default false)
    :editable?    If false, the text is read-only (default true)
    :margin
    :caret-color          Color of caret (see seesaw.color)
    :caret-position       Caret position as zero-based integer offset
    :disabled-text-color  A color value
    :selected-text-color  A color value
    :selection-color      A color value


  The following properties only apply if :multi-line? is false:

    :columns Number of columns of text
    :halign  Horizontal text alignment (:left, :right, :center, :leading, :trailing)

  The following properties only apply if :multi-line? is true:

    :wrap-lines?  If true (and :multi-line? is true) lines are wrapped.
                  (default false)
    :tab-size     Tab size in spaces. Defaults to 8. Only applies if :multi-line?
                  is true.
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

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JTextArea.html
    http://download.oracle.com/javase/6/docs/api/javax/swing/JTextField.html
  "
  [& args]
  (if (= 1 (count args))
    (let [arg0      (first args)
          as-doc    (to-document arg0)
          as-widget (to-widget arg0)
          multi?    (or (coll? arg0) (seq? arg0))]
      (cond
        (nil? arg0) (illegal-argument "First arg must not be nil")
        as-doc      (get-text as-doc)
        as-widget   (get-text as-widget)
        multi?      (map #(text %) arg0)
        :else       (text :text arg0)))
    (let [{:keys [multi-line?] :as opts} args
          opts (dissoc opts :multi-line?)]
      (if multi-line?
        (apply-options (construct JTextArea) opts)
        (apply-options (construct JTextField) opts)))))

(defn text!
  "Set the text of widget(s) or document(s). targets is an object that can be
  turned into a widget or document, or a list of such things. value is the new
  text value to be applied. Returns targets.

  target may be one of:

    A widget
    A widget-able thing like an event
    A Document
    A DocumentEvent

  The resulting text in the widget depends on the type of value:

    A string                               - the string
    A URL, File, or anything \"slurpable\" - the slurped value
    Anythign else                          - (resource value)

  Example:

      user=> (def t (text \"HI\"))
      user=> (text! t \"BYE\")
      user=> (text t)
      \"BYE\"

      ; Put the contents of a URL in editor
      (text! editor (java.net.URL. \"http://google.com\"))

  Notes:

    This applies to the :text property of new text widgets and config! as well.
  "
  [targets value]
  (check-args (not (nil? targets)) "First arg must not be nil")
  (doseq [w (to-seq targets)]
    (set-text w value))
  targets)

(defn- add-styles [^JTextPane text-pane styles]
  (doseq [[id & options] styles]
    (let [style (.addStyle text-pane (name id) nil)]
      (doseq [[k v] (partition 2 options)]
        (case k
          :font       (.addAttribute style StyleConstants/FontFamily (name v))
          :size       (.addAttribute style StyleConstants/FontSize (Integer. v))
          :color      (.addAttribute style StyleConstants/Foreground (seesaw.color/to-color v))
          :background (.addAttribute style StyleConstants/Background (seesaw.color/to-color v))
          :bold       (.addAttribute style StyleConstants/Bold (boolean v))
          :italic     (.addAttribute style StyleConstants/Italic (boolean v))
          :underline  (.addAttribute style StyleConstants/Underline (boolean v))
          (illegal-argument "Option %s is not supported in :styles" k))))))

(def styled-text-options
  (merge
    text-options
    (option-map
      (default-option :wrap-lines? #(put-meta! %1 :wrap-lines? (boolean %2))
                     #(get-meta %1 :wrap-lines?))
      (default-option :styles add-styles))))

(widget-option-provider javax.swing.JTextPane styled-text-options)

(defn styled-text
  "Create a text pane.
  Supports the following options:

    :text         text content.
    :wrap-lines?  If true wraps lines.
                  This only works if the styled text is wrapped
                  in (seesaw.core/scrollable). Doing so will cause
                  a grey area to appear to the right of the text.
                  This can be avoided by calling
                    (.setBackground (.getViewport s) java.awt.Color/white)
                  on the scrollable s.
    :styles       Define styles, should be a list of vectors of form:
                  [identifier & options]
                  Where identifier is a string or keyword
                  Options supported:
                    :font        A font family name as keyword or string.
                    :size        An integer.
                    :color       See (seesaw.color/to-color)
                    :background  See (seesaw.color/to-color)
                    :bold        bold if true.
                    :italic      italic if true.
                    :underline   underline if true.

  See:
    (seesaw.core/style-text!)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JTextPane.html
  "
  { :arglists '([& args]) }
  [& {:as opts}]
  (let [pane (proxy [JTextPane] []
               (getScrollableTracksViewportWidth []
                 (boolean (get-meta this :wrap-lines?))))]
    (apply-options pane opts)))

(defn style-text!
  "Style a JTextPane
  id identifies a style that has been added to the text pane.

  See:

    (seesaw.core/text)
    http://download.oracle.com/javase/tutorial/uiswing/components/editorpane.html
  "
  [^JTextPane target id ^Integer start ^Integer length]
  (check-args (instance? JTextPane target) "style-text! only applied to styled-text widgets")
  (.setCharacterAttributes (.getStyledDocument target)
                            start length (.getStyle target (name id)) true)
  target)

;*******************************************************************************
; JPasswordField

(def password-options
  (merge
    text-field-options
    (option-map
      (bean-option :echo-char javax.swing.JPasswordField))))

(widget-option-provider javax.swing.JPasswordField password-options)

(defn password
  "Create a password field. Options are the same as single-line text fields with
  the following additions:

    :echo-char The char displayed for the characters in the password field

  Returns an instance of JPasswordField.

  Example:

    (password :echo-char \\X)

  Notes:

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JPasswordField.html
  "
  [& opts]
  (let [pw (construct javax.swing.JPasswordField)]
    (apply-options pw opts)))

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

(def editor-pane-options
  (merge
    text-options
    (option-map
      (bean-option :page javax.swing.JEditorPane to-url)
      (bean-option :content-type javax.swing.JEditorPane str)
      (bean-option :editor-kit javax.swing.JEditorPane))))

(widget-option-provider javax.swing.JEditorPane editor-pane-options)

(defn editor-pane
  "Create a JEditorPane. Custom options:

    :page         A URL (string or java.net.URL) with the contents of the editor
    :content-type The content-type, for example \"text/html\" for some crappy
                  HTML rendering.
    :editor-kit   The EditorKit. See Javadoc.

  Notes:

    An editor pane can fire 'hyperlink' events when elements are click,
    say like a hyperlink in an html doc. You can listen to these with the
    :hyperlink event:

      (listen my-editor :hyperlink (fn [e] ...))

    where the event is an instance of javax.swing.event.HyperlinkEvent.
    From there you can inspect the event, inspect the clicked element,
    etc.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JEditorPane.html
    http://docs.oracle.com/javase/6/docs/api/javax/swing/event/HyperlinkEvent.html
  "
  [& opts]
  (apply-options (construct javax.swing.JEditorPane) opts))

;*******************************************************************************
; Listbox

(defn- to-list-model [xs]
  (if (instance? javax.swing.ListModel xs)
    xs
    (let [model (javax.swing.DefaultListModel.)]
      (doseq [x xs]
        (.addElement model x))
      model)))

(def listbox-options
  (merge
    default-options
    (option-map
      (around-option model-option to-list-model identity "See (seesaw.core/listbox)")
      (default-option :renderer
                        #(.setCellRenderer ^javax.swing.JList %1 (seesaw.cells/to-cell-renderer %1 %2))
                        #(.getCellRenderer ^javax.swing.JList %1))
      (selection-mode-option list-selection-mode-table)
      (bean-option :fixed-cell-height javax.swing.JList)
      (layout-orientation-option list-layout-orientation-table)
      (bean-option :drop-mode javax.swing.JList keyword-to-drop-mode drop-mode-to-keyword (keys keyword-to-drop-mode)))))

(widget-option-provider javax.swing.JList listbox-options)

(defn listbox
  "Create a list box (JList). Additional options:

    :model A ListModel, or a sequence of values with which a DefaultListModel
           will be constructed.
    :renderer A cell renderer to use. See (seesaw.cells/to-cell-renderer).

  Notes:

    Retrieving and setting the current selection of the list box is fully
    supported by the (selection) and (selection!) functions.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JList.html
  "
  [& args]
  (apply-options (construct javax.swing.JList) args))

;*******************************************************************************
; JTable

(defn- to-table-model [v]
  (cond
    (instance? javax.swing.table.TableModel v) v
    :else (apply seesaw.table/table-model v)))

(defn- table-columns [^javax.swing.JTable table]
  (-> table .getColumnModel .getColumns enumeration-seq))

(def ^{:private true} auto-resize-mode-table {
  :off                javax.swing.JTable/AUTO_RESIZE_OFF
  :next-column        javax.swing.JTable/AUTO_RESIZE_NEXT_COLUMN
  :subsequent-columns javax.swing.JTable/AUTO_RESIZE_SUBSEQUENT_COLUMNS
  :last-column        javax.swing.JTable/AUTO_RESIZE_LAST_COLUMN
  :all-columns        javax.swing.JTable/AUTO_RESIZE_ALL_COLUMNS
})

(def table-options
  (merge
    default-options
    (option-map
      model-option
      (bean-option :model javax.swing.JTable to-table-model)
      (default-option :show-grid?
                                #(.setShowGrid ^javax.swing.JTable %1 (boolean %2))
                                (fn [^javax.swing.JTable t]
                                  (and (.getShowHorizontalLines t)
                                      (.getShowVerticalLines t))))
      (default-option :column-widths
                                   #(doall
                                     (map (fn [c w] (.setWidth c w) (.setPreferredWidth c w)) (table-columns %1) %2))
                                   #(doall
                                     (map (fn [c] (.getWidth c)) (table-columns %1))))
      (bean-option [:show-vertical-lines? :show-vertical-lines] javax.swing.JTable boolean)
      (bean-option [:show-horizontal-lines? :show-horizontal-lines] javax.swing.JTable boolean)
      (bean-option [:fills-viewport-height? :fills-viewport-height] javax.swing.JTable boolean)
      (selection-mode-option list-selection-mode-table)
      (bean-option [:auto-resize :auto-resize-mode] javax.swing.JTable auto-resize-mode-table nil (keys auto-resize-mode-table))
      (bean-option :drop-mode javax.swing.JTable keyword-to-drop-mode drop-mode-to-keyword (keys keyword-to-drop-mode)))))

(widget-option-provider javax.swing.JTable table-options)

(defn table
  "Create a table (JTable). Additional options:

    :model A TableModel, or a vector. If a vector, then it is used as
           arguments to (seesaw.table/table-model).
    :show-grid? Whether to show the grid lines of the table.
    :show-horizontal-lines? Whether to show horizontal grid lines
    :show-vertical-lines?   Whether to show vertical grid lines
    :fills-viewport-height?
    :auto-resize The behavior of columns when the table is resized. One of:
           :off                Do nothing to column widths
           :next-column        When a column is resized, take space from next column
           :subsequent-columns Change subsequent columns to presercve total width of table
           :last-column        Apply adjustments to last column only
           :all-columns        Proportionally resize all columns
      Defaults to :subsequent-columns. If you're wondering where your horizontal scroll
      bar is, try setting this to :off.

  Example:

    (table
      :model [:columns [:age :height]
              :rows    [{:age 13 :height 45}
                        {:age 45 :height 13}]])

  Notes:

  See:
    seesaw.table/table-model
    seesaw.examples.table
    http://download.oracle.com/javase/6/docs/api/javax/swing/JTable.html"
  [& args]
  (apply-options
    (doto ^javax.swing.JTable (construct javax.swing.JTable)
      (.setFillsViewportHeight true))
    args))

;*******************************************************************************
; JTree

(def tree-options
  (merge
    default-options
    (option-map
      model-option
      (bean-option :editable? javax.swing.JTree boolean)
      (default-option :renderer
                      #(.setCellRenderer ^javax.swing.JTree %1 (seesaw.cells/to-cell-renderer %1 %2))
                      #(.getCellRenderer ^javax.swing.JTree %1))
      (bean-option [:expands-selected-paths? :expands-selected-paths] javax.swing.JTree boolean)
      (bean-option :large-model? javax.swing.JTree boolean)
      (bean-option :root-visible? javax.swing.JTree boolean)
      (bean-option :row-height javax.swing.JTree)
      (bean-option [:scrolls-on-expand? :scrolls-on-expand] javax.swing.JTree boolean)
      (bean-option [:shows-root-handles? :shows-root-handles] javax.swing.JTree boolean)
      (bean-option :toggle-click-count javax.swing.JTree)
      (bean-option :visible-row-count javax.swing.JTree)
      (selection-mode-option tree-selection-mode-table)
      (bean-option :drop-mode javax.swing.JTree keyword-to-drop-mode drop-mode-to-keyword (keys keyword-to-drop-mode)))))

(widget-option-provider javax.swing.JTree tree-options)

(defn tree
  "Create a tree (JTree). Additional options:

  Notes:

  See:

    http://download.oracle.com/javase/6/docs/api/javax/swing/JTree.html
  "
  [& args]
  (apply-options (construct javax.swing.JTree) args))

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

(def combobox-options
  (merge
    default-options
    (option-map
      action-option
      (bean-option :editable? javax.swing.JComboBox boolean)
      (bean-option :selected-item javax.swing.JComboBox)
      (bean-option :selected-index javax.swing.JComboBox)
      (around-option model-option to-combobox-model identity "See (seesaw.core/combobox)")
      (default-option :renderer
                      #(.setRenderer ^javax.swing.JComboBox %1 (seesaw.cells/to-cell-renderer %1 %2))
                      #(.getRenderer ^javax.swing.JComboBox %1)))))

(widget-option-provider javax.swing.JComboBox combobox-options)

(defn combobox
  "Create a combo box (JComboBox). Additional options:

    :model Instance of ComboBoxModel, or sequence of values used to construct
           a default model.
    :renderer Cell renderer used for display. See (seesaw.cells/to-cell-renderer).

  Note that the current selection can be retrieved and set with the (selection) and
  (selection!) functions. Calling (seesaw.core/text) on a combobox will return
  (str (selection cb)). (seesaw.core/text!) is not supported.

  Notes:

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JComboBox.html
  "
  [& args]
  (apply-options (construct javax.swing.JComboBox) args))

(def ^{:private true} spinner-date-by-table
  (constant-map java.util.Calendar
    :era
    :year
    :month
    :week-of-year
    :week-of-month
    :day-of-month
    :day-of-year
    :day-of-week
    :day-of-week-in-month
    :am-pm
    :hour
    :hour-of-day
    :minute
    :second
    :millisecond))

(defn ^javax.swing.SpinnerModel spinner-model
  "A helper function for creating spinner models. Calls take the general
  form:

      (spinner-model initial-value
        :from start-value :to end-value :by step)

  Values can be one of:

    * java.util.Date where step is one of :day-of-week, etc. See
      java.util.Calendar constants.
    * a number

  Any of the options beside the initial value may be omitted.

  Note that on some platforms the :by parameter will be ignored for date
  spinners.

  See:
    (seesaw.core/spinner)
    http://download.oracle.com/javase/6/docs/api/javax/swing/SpinnerDateModel.html
    http://download.oracle.com/javase/6/docs/api/javax/swing/SpinnerNumberModel.html
    http://download.oracle.com/javase/6/docs/api/javax/swing/JSpinner.html
  "
  [v & {:keys [from to by]}]
  (cond
    ; TODO Reflection here. Don't know how to get rid of it.
    (number? v) 
    (let [step (or by 1)] 
      (javax.swing.SpinnerNumberModel. ^Number v ^Comparable from ^Comparable to 
                                       ^Number step))
    (instance? java.util.Date v)
      (javax.swing.SpinnerDateModel. ^java.util.Date v
                                     from to
                                     (spinner-date-by-table by))
    :else (illegal-argument "Don't' know how to make spinner :model from %s" (class v))))

(defn- ^javax.swing.SpinnerModel to-spinner-model [v]
  (cond
    (instance? javax.swing.SpinnerModel v) v
    (sequential? v)                        (javax.swing.SpinnerListModel. ^java.util.List v)
    (instance? java.util.Date v) (doto (javax.swing.SpinnerDateModel.) (.setValue ^java.util.Date v))
    (number? v) (doto (javax.swing.SpinnerNumberModel.) (.setValue v))
    :else (illegal-argument "Don't' know how to make spinner :model from %s" (class v))))

(def spinner-options
  (merge
    default-options
    (option-map
      (around-option model-option to-spinner-model identity "See (seesaw.core/spinner)"))))

(widget-option-provider javax.swing.JSpinner spinner-options)

(defn spinner
  "Create a spinner (JSpinner). Additional options:

    :model Instance of SpinnerModel, or one of the values described below.

  Note that the value can be retrieved and set with the (selection) and
  (selection!) functions. Listen to :selection to be notified of value
  changes.

  The value of model can be one of the following:

    * An instance of javax.swing.SpinnerModel
    * A java.util.Date instance in which case the spinner starts at that date,
      is unbounded, and moves by day.
    * A number giving the initial value for an unbounded number spinner
    * A value returned by (seesaw.core/spinner-model)

  Notes:

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JSpinner.html
    http://download.oracle.com/javase/6/docs/api/javax/swing/SpinnerModel.html
    (seesaw.core/spinner-model)
    test/seesaw/test/examples/spinner.clj
  "
  [& args]
  (apply-options (construct javax.swing.JSpinner) args))

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

(def ^{:private true} scrollable-corner-constants {
  :lower-left  ScrollPaneConstants/LOWER_LEFT_CORNER
  :lower-right ScrollPaneConstants/LOWER_RIGHT_CORNER
  :upper-left  ScrollPaneConstants/UPPER_LEFT_CORNER
  :upper-right ScrollPaneConstants/UPPER_RIGHT_CORNER
})

(defn- set-scrollable-corner [k ^JScrollPane w v]
  (.setCorner w (scrollable-corner-constants k) (make-widget v)))

(def scrollable-options
  (merge
    default-options
    (option-map
      (bean-option [:hscroll :horizontal-scroll-bar-policy] JScrollPane hscroll-table)
      (bean-option [:vscroll :vertical-scroll-bar-policy] JScrollPane vscroll-table)
      (default-option :row-header
        (fn [^JScrollPane w v]
          (let [v (make-widget v)]
          (if (instance? javax.swing.JViewport v)
            (.setRowHeader w v)
            (.setRowHeaderView w v)))))
      (default-option :column-header
        (fn [^JScrollPane w v]
          (let [v (make-widget v)]
            (if (instance? javax.swing.JViewport v)
              (.setColumnHeader w v)
              (.setColumnHeaderView w v))))))
   (apply option-map
          (for [k (keys scrollable-corner-constants)]
             (default-option k (partial set-scrollable-corner k))))))

(widget-option-provider JScrollPane scrollable-options)

(defn scrollable
  "Wrap target in a JScrollPane and return the scroll pane.

  The first argument is always the widget that should be scrolled. It's followed
  by zero or more options *for the scroll pane*.

  Additional Options:

    :hscroll       - Controls appearance of horizontal scroll bar.
                     One of :as-needed (default), :never, :always
    :vscroll       - Controls appearance of vertical scroll bar.
                     One of :as-needed (default), :never, :always
    :row-header    - Row header widget or viewport
    :column-header - Column header widget or viewport
    :lower-left    - Widget in lower-left corner
    :lower-right   - Widget in lower-right corner
    :upper-left    - Widget in upper-left corner
    :upper-right   - Widget in upper-right corner

  Examples:

    ; Vanilla scrollable
    (scrollable (listbox :model [\"Foo\" \"Bar\" \"Yum\"]))

    ; Scrollable with some options on the JScrollPane
    (scrollable (listbox :model [\"Foo\" \"Bar\" \"Yum\"]) :id :#scrollable :border 5)

  Notes:

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JScrollPane.html
  "
  [target & opts]
  (let [^JScrollPane sp (construct JScrollPane)]
    (.setViewportView sp (make-widget target))
    (apply-options sp opts)))

(defn scroll!
  "Scroll a widget. Obviously, the widget must be contained in a scrollable.
  Returns the widget.

  The basic format of the function call is:

    (scroll! widget modifier argument)

  widget is passed through (to-widget) as usual. Currently, the only accepted
  value for modifier is :to. The interpretation and set of accepted values for
  argument depends on the type of widget:

    All Widgets:

      :top           - Scroll to the top of the widget
      :bottom        - Scroll to the bottom of the widget
      java.awt.Point - Scroll so the given pixel point is visible
      java.awt.Rectangle - Scroll so the given rectangle is visible
      [:point x y]   - Scroll so the given pixel point is visible
      [:rect x y w h] - Scroll so the given rectable is visible

    listboxes (JList):

      [:row n] - Scroll so that row n is visible

    tables (JTable):

      [:row n]        - Scroll so that row n is visible
      [:column n]     - Scroll so that column n is visible
      [:cell row col] - Scroll so that the given cell is visible

    text widgets:

      [:line n] - Scroll so that line n is visible
      [:position n] - Scroll so that position n (character offset) is visible

      Note that for text widgets, the caret will also be moved which in turn
      causes the selection to change.

  Examples:

    (scroll! w :to :top)
    (scroll! w :to :bottom)
    (scroll! w :to [:point 99 10])
    (scroll! w :to [:rect  99 10 100 100])

    (scroll! listbox :to [:row 99])

    (scroll! table :to [:row 99])
    (scroll! table :to [:column 10])
    (scroll! table :to [:cell 99 10])

    (scroll! text :to [:line 200])
    (scroll! text :to [:position 2000])

  See:
    (seesaw.scroll/scroll!*)
    (seesaw.examples.scroll)
  "
  [target modifier arg]
  (seesaw.scroll/scroll!* (to-widget target) modifier arg)
  target)

;*******************************************************************************
; Splitter

(defn- divider-location-proportional!
  [^javax.swing.JSplitPane splitter value]
  (if (.isShowing splitter)
    (if (and (> (.getWidth splitter) 0) (> (.getHeight splitter) 0))
      (.setDividerLocation splitter (double value))
      (.addComponentListener splitter
        (proxy [java.awt.event.ComponentAdapter] []
          (componentResized [e]
            (.removeComponentListener splitter this)
            (divider-location-proportional! splitter value)))))
    (.addHierarchyListener splitter
      (reify java.awt.event.HierarchyListener
        (hierarchyChanged [this e]
          (when (and (not= 0 (bit-and
                               ^Integer (.getChangeFlags e)
                               ^Integer java.awt.event.HierarchyEvent/SHOWING_CHANGED))
                   (.isShowing splitter))
            (.removeHierarchyListener splitter this)
            (divider-location-proportional! splitter value)))))))

(defn- divider-location!
  "Sets the divider location of a splitter. Value can be one of cases:

    integer - Treated as an absolute pixel size
    double or rational - Treated as a percentage of the splitter's size

  Use the :divider-location property to set this at creation time of a
  splitter.

  Returns the splitter.

  Notes:

    This function fixes the well known limitation of JSplitPane that it will
    basically ignore proportional sizes if the splitter isn't visible yet.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JSplitPane.html#setDividerLocation%28double%29
    http://blog.darevay.com/2011/06/jsplitpainintheass-a-less-abominable-fix-for-setdividerlocation/
  "
  [^javax.swing.JSplitPane splitter value]
  (cond
    (integer? value) (.setDividerLocation splitter ^Integer value)
    (ratio?   value) (divider-location! splitter (double value))
    (float?   value) (divider-location-proportional! splitter value)
    :else (illegal-argument "Expected integer or float, got %s" value))
  splitter)

(def splitter-options
  (merge
    default-options
    (option-map
      (default-option :divider-location divider-location! #(.getDividerLocation ^JSplitPane %1))
      (bean-option :divider-size JSplitPane)
      (bean-option :resize-weight JSplitPane)
      (bean-option :one-touch-expandable? JSplitPane boolean))))

(widget-option-provider JSplitPane splitter-options)

(defn splitter
  "
  Create a new JSplitPane. This is a lower-level function. Usually you want
  (seesaw.core/top-bottom-split) or (seesaw.core/left-right-split). But here's
  the additional options any three of these functions can take:

    :divider-location The initial divider location. See (seesaw.core/divider-location!).

  Notes:

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JSplitPane.html
  "
  [dir left right & opts]
  (apply-options
    (doto ^JSplitPane (construct JSplitPane)
      (.setOrientation (dir {:left-right JSplitPane/HORIZONTAL_SPLIT
                             :top-bottom JSplitPane/VERTICAL_SPLIT}))
      (.setLeftComponent (make-widget left))
      (.setRightComponent (make-widget right)))
    opts))

(def left-right-split-options splitter-options)

(defn left-right-split
  "Create a left/right (horizontal) splitpane with the given widgets. See
  (seesaw.core/splitter) for additional options. Options are given after
  the two widgets.

  Notes:

  See:
    (seesaw.core/splitter)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JSplitPane.html
  "
  [left right & args]
  (apply splitter :left-right left right args))

(def top-bottom-split-options splitter-options)

(defn top-bottom-split
  "Create a top/bottom (vertical) split pane with the given widgets. See
  (seesaw.core/splitter) for additional options. Options are given after
  the two widgets.

  Notes:

  See:
    (seesaw.core/splitter)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JSplitPane.html
  "
  [top bottom & args]
  (apply splitter :top-bottom top bottom args))

;*******************************************************************************
; Separator
(def separator-options
  (merge
    default-options
    (option-map
      (bean-option :orientation javax.swing.JSeparator orientation-table))))

(widget-option-provider javax.swing.JSeparator separator-options)

(defn separator
  "Create a separator.

  Notes:

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JSeparator.html
  "
  [& opts]
  (apply-options (construct javax.swing.JSeparator) opts))

;*******************************************************************************
; Menus

(def menu-item-options
  (merge
    button-options
    (option-map
      (bean-option [:key :accelerator] javax.swing.JMenuItem seesaw.keystroke/keystroke))))

(widget-option-provider javax.swing.JMenuItem menu-item-options)

(defn menu-item
  "Create a menu item for use in (seesaw.core/menu). Supports same options as
  (seesaw.core/button)"
  [& args]
  (apply-options (javax.swing.JMenuItem.) args))

(def checkbox-menu-item-options menu-item-options)

(defn checkbox-menu-item
  "Create a checked menu item for use in (seesaw.core/menu). Supports same options as
  (seesaw.core/button)"
  [& args]
  (apply-options (javax.swing.JCheckBoxMenuItem.) args))

(def radio-menu-item-options menu-item-options)

(defn
  radio-menu-item
  "Create a radio menu item for use in (seesaw.core/menu). Supports same options as
  (seesaw.core/button).

  Notes:
    Use (seesaw.core/button-group) or the :group option to enforce mutual exclusion
    across menu items."
  [& args] (apply-options (javax.swing.JRadioButtonMenuItem.) args))

(defn- ^javax.swing.JMenuItem to-menu-item
  [item]
  ; TODO this sucks
  (if (instance? javax.swing.Action item)
    (javax.swing.JMenuItem. ^javax.swing.Action item)
    (if-let [^javax.swing.Icon icon (make-icon item)]
      (javax.swing.JMenuItem. icon)
      (if (instance? String item)
        (javax.swing.JMenuItem. ^String item)))))

(def menu-options
  (merge
    button-options
    (option-map
      (default-option :items
              (fn [^javax.swing.JMenu menu items]
                (doseq [item items]
                  (if-let [menu-item (to-menu-item item)]
                    (.add menu menu-item)
                    (if (= :separator item)
                      (.addSeparator menu)
                      (.add menu (make-widget item))))))))))

(widget-option-provider javax.swing.JMenu menu-options)

(defn menu
  "Create a new menu. In addition to all options applicable to (seesaw.core/button)
  the following additional options are supported:

    :items Sequence of menu item-like things (actions, icons, JMenuItems, etc)

  Notes:

  See:
    (seesaw.core/button)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JMenu.html"
  [& opts]
  (apply-options (construct javax.swing.JMenu) opts))

(def popup-options
  (merge
    default-options
    (option-map
      ; TODO reflection - duplicate of menu-options
      (default-option :items
        (fn [^javax.swing.JPopupMenu menu items]
          (doseq [item items]
            (if-let [menu-item (to-menu-item item)]
              (.add menu menu-item)
              (if (= :separator item)
                (.addSeparator menu)
                (.add menu (make-widget item))))))))))

(widget-option-provider javax.swing.JPopupMenu popup-options)

(defn popup
  "Create a new popup menu. Additional options:

    :items Sequence of menu item-like things (actions, icons, JMenuItems, etc)

  Note that in many cases, the :popup option is what you want if you want to
  show a context menu on a widget. It handles all the yucky mouse stuff and
  fixes various eccentricities of Swing.

  Notes:

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JPopupMenu.html"
  [& opts]
  (apply-options (construct javax.swing.JPopupMenu) opts))


(defn- ^javax.swing.JPopupMenu make-popup [target arg event]
  (cond
    (instance? javax.swing.JPopupMenu arg) arg
    (fn? arg)                              (popup :items (arg event))
    :else (illegal-argument "Don't know how to make popup with %s" arg)))

(defn- popup-option-handler
  [^java.awt.Component target arg]
  (listen target :mouse
    (fn [^java.awt.event.MouseEvent event]
      (when (.isPopupTrigger event)
        (let [p (make-popup target arg event)]
          (.show p (to-widget event) (.x (.getPoint event)) (.y (.getPoint event))))))))


(def menubar-options
  (merge
    default-options
    (option-map
      layout/default-items-option)))

(widget-option-provider javax.swing.JMenuBar menubar-options)

(defn menubar
  "Create a new menu bar, suitable for the :menubar property of (frame).
  Additional options:

    :items Sequence of menus, see (menu).

  Notes:

  See:
    (seesaw.core/frame)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JMenuBar.html
  "
  [& opts]
  (apply-options (construct javax.swing.JMenuBar) opts))

;*******************************************************************************
; Toolbars


(defn- insert-toolbar-separators
  "Replace :separator with JToolBar$Separator instances"
  [items]
  (map #(if (= % :separator) (javax.swing.JToolBar$Separator.) %) items))

(def toolbar-options
  (merge
    default-options
    (option-map
      (bean-option :orientation javax.swing.JToolBar orientation-table)
      (bean-option :floatable? javax.swing.JToolBar boolean)
      ; Override default :items handler
      (default-option :items
        #(layout/add-widgets %1 (insert-toolbar-separators %2))))))

(widget-option-provider javax.swing.JToolBar toolbar-options)

(defn toolbar
  "Create a JToolBar. The following properties are supported:

    :floatable?  Whether the toolbar is floatable.
    :orientation Toolbar orientation, :horizontal or :vertical
    :items       Normal list of widgets to add to the toolbar. :separator
                 creates a toolbar separator.

  Notes:

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JToolBar.html
  "
  [& opts]
  (apply-options (construct javax.swing.JToolBar) opts))

;*******************************************************************************
; Tabs

(def ^{:private true} tab-placement-table
  (constant-map SwingConstants :bottom :top :left :right))

(def ^{:private true} tab-overflow-table {
  :scroll JTabbedPane/SCROLL_TAB_LAYOUT
  :wrap   JTabbedPane/WRAP_TAB_LAYOUT
})

(defn- add-to-tabbed-panel
  [^javax.swing.JTabbedPane tp tab-defs]
  (doseq [{:keys [title content tip icon]} tab-defs]
    (let [title-cmp (try-cast Component title)
          index     (.getTabCount tp)]
      (cond-doto tp
        true (.addTab (if-not title-cmp (resource title)) (make-icon icon) (make-widget content) (resource tip))
        title-cmp (.setTabComponentAt index title-cmp))))
  tp)

(def tabbed-panel-options
  (merge
    default-options
    (option-map
      (bean-option [:placement :tab-placement] javax.swing.JTabbedPane tab-placement-table)
      (bean-option [:overflow :tab-layout-policy] javax.swing.JTabbedPane tab-overflow-table)
      (default-option :tabs add-to-tabbed-panel))))

(widget-option-provider javax.swing.JTabbedPane tabbed-panel-options)

(defn tabbed-panel
  "Create a JTabbedPane. Supports the following properties:

    :placement Tab placement, one of :bottom, :top, :left, :right.
    :overflow  Tab overflow behavior, one of :wrap, :scroll.
    :tabs      A list of tab descriptors. See below

  A tab descriptor is a map with the following properties:

    :title     Title of the tab or a component to be displayed.
    :tip       Tab's tooltip text
    :icon      Tab's icon, passed through (icon)
    :content   The content of the tab, passed through (make-widget) as usual.

  Returns the new JTabbedPane.

  Notes:

  The currently selected tab can be retrieved with the (selection) function.
  It returns a map similar to the tab descriptor with keys :title, :content,
  and :index.

  Similarly, a tab can be programmatically selected with the
  (selection!) function, by passing one of the following values:

    * A number - The index of the tab to select
    * A string - The title of the tab to select
    * A to-widget-able - The content of the tab to select
    * A map as returned by (selection) with at least an :index, :title, or
      :content key.

  Furthermore, you can be notified for when the active tab changes by
  listening for the :selection event:

    (listen my-tabbed-panel :selection (fn [e] ...))

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JTabbedPane.html
    (seesaw.core/selection)
    (seesaw.core/selection!)
  "
  [& opts]
  (apply-options (construct javax.swing.JTabbedPane) opts))

;*******************************************************************************
; Canvas

(def ^{:private true} paint-property "seesaw-paint")

(defn- paint-component-impl [^javax.swing.JComponent this ^java.awt.Graphics2D g]
  (let [{:keys [before after super?] :or {super? true}} (get-meta this paint-property)]
    (seesaw.graphics/anti-alias g)
    (when before (seesaw.graphics/push g (before this g)))
    ; TODO reflection here can't be eliminated thanks for proxy limitations
    ; with protected methods
    (when super? (proxy-super paintComponent g))
    (when after  (seesaw.graphics/push g (after this g)))))

(defn- paint-option-handler [^java.awt.Component c v]
  (cond
    (nil? v) (do
               (update-proxy c {"paintComponent" nil})
               (.repaint c))
    (fn? v)  (paint-option-handler c {:after v})
    (map? v) (do
               (put-meta! c paint-property v)
               (update-proxy c {"paintComponent" paint-component-impl})
               (.repaint c))
    :else (illegal-argument "Expect map or function for :paint property")))

(defmacro paintable
  "*Deprecated. Just use :paint directly on any widget.*

  Macro that generates a paintable widget, i.e. a widget that can be drawn on
  by client code. target is a Swing class literal indicating the type that will
  be constructed.

  All other options will be passed along to the given Seesaw widget
  as usual and will be applied to the generated class.

  Notes:
    If you just want a panel to draw on, use (seesaw.core/canvas). This macro is
    intended for customizing the appearance of existing widget types.

  Examples:

    ; Create a raw JLabel and paint over it.
    (paintable javax.swing.JLabel :paint (fn [c g] (.fillRect g 0 0 20 20))

  See:
    (seesaw.core/canvas)
    (seesaw.graphics)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JComponent.html#paintComponent%28java.awt.Graphics%29
 "
 [cls & opts]
 `(apply-options (construct ~cls) (vector ~@opts)))

(def canvas-options default-options)

(defn canvas
  "Creates a paintable canvas, i.e. a JPanel with paintComponent overridden.
  Painting is configured with the :paint property which can take the
  following values:

    nil - disables painting. The widget will be filled with its background
      color unless it is not opaque.

    (fn [c g]) - a paint function that takes the widget and a Graphics2D as
      arguments. Called after super.paintComponent.

    {:before fn :after fn :super? bool} - a map with :before and :after functions which
      are called before and after super.paintComponent respectively. If super?
      is false, super.paintComponent is not called.

  Notes:

    The :paint option is actually supported by *all* Seesaw widgets.

    (seesaw.core/config!) can be used to change the :paint property at any time.

    Some customizations are also possible and maybe easier with
    the creative use of borders.

  Examples:

    (canvas :paint #(.drawString %2 \"I'm a canvas\" 10 10))

  See:
    (seesaw.graphics)
    (seesaw.examples.canvas)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JComponent.html#paintComponent%28java.awt.Graphics%29
  "
  [& opts]
  (let [{:keys [paint] :as opts} opts
        ^javax.swing.JPanel p (construct javax.swing.JPanel)]
    (.setLayout p nil)
    (apply-options p opts)))
;
;*******************************************************************************
; Window

; base options shared by top-level stuff.
(def ^{:private true} abstract-window-options
  (option-map
    (default-option :id
      seesaw.selector/id-of!
      seesaw.selector/id-of
      ["A keyword id."
       "See (seesaw.core/select)"])

    (default-option :class
      seesaw.selector/class-of!
      seesaw.selector/class-of
      ["A keyword class, in the HTML/CSS sense."
       "See (Seesaw.core/select)"])

    (default-option
      :content
      (fn [^javax.swing.RootPaneContainer f v]
        (doto f
          (.setContentPane (make-widget v))
          .invalidate
          .validate
          .repaint))
      (fn [^javax.swing.RootPaneContainer f] (.getContentPane f))
      "The frame's main content widget")

    (bean-option :minimum-size  java.awt.Window to-dimension nil
                 dimension-examples)

    (bean-option :size java.awt.Window to-dimension nil
                 dimension-examples)

    (bean-option :visible? java.awt.Window boolean)
    ; TODO reflection. transfer-handler is in JWindow, JDialog, and JFrame, not a common
    ; base or interface.
    (bean-option :transfer-handler java.awt.Window
                 seesaw.dnd/to-transfer-handler
                 identity
                 "See (seesaw.dnd/to-transfer-handler)") ))

(def window-options abstract-window-options)

(option-provider javax.swing.JWindow window-options)

(defn window
  "Create a JWindow. NOTE: A JWindow is a top-level window with no decorations,
  i.e. no title bar, no menu, no nothin'. Usually you want (seesaw.core/frame)
  if your just showing a normal top-level app.

  Options:

    :id       id of the window, used by (select).
    :width    initial width. Note that calling (pack!) will negate this setting
    :height   initial height. Note that calling (pack!) will negate this setting
    :size     initial size. Note that calling (pack!) will negate this setting
    :minimum-size minimum size of frame, e.g. [640 :by 480]
    :content  passed through (make-widget) and used as the frame's content-pane
    :visible?  whether frame should be initially visible (default false)

  returns the new window

  Examples:

    ; Create a window, pack it and show it.
    (-> (window :content \"I'm a label!\")
      pack!
      show!)

    ; Create a frame with an initial size (note that pack! isn't called)
    (show! (window :content \"I'm a label!\" :width 500 :height 600))

  Notes:
    Unless :visible? is set to true, the window will not be displayed until (show!)
    is called on it.

    Call (pack!) on the frame if you'd like the window to resize itself to fit its
    contents. Sometimes this doesn't look like crap.

  See:
    (seesaw.core/show!)
    (seesaw.core/hide!)
    (seesaw.core/move!)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JWindow.html
  "
  [& {:keys [width height visible? size]
      :as opts}]
  (cond-doto ^javax.swing.JWindow (apply-options (construct javax.swing.JWindow)
                                    (dissoc opts :width :height :visible?))
    (and (not size)
         (or width height)) (.setSize (or width 100) (or height 100))
    visible?   (.setVisible (boolean visible?))))

;*******************************************************************************
; Frame

(defn- default-screen-device []
  (->
    (java.awt.GraphicsEnvironment/getLocalGraphicsEnvironment)
    .getDefaultScreenDevice))

(defn full-screen-window
  "Returns the window/frame that is currently in full-screen mode or nil if
  none."
  ([^java.awt.GraphicsDevice device]
    (.getFullScreenWindow device))
  ([]
    (full-screen-window (default-screen-device))))

(defn full-screen?
  "Returns true if the given window/frame is in full-screen mode"
  ([^java.awt.GraphicsDevice device window]
    (= (to-root window) (.getFullScreenWindow device)))
  ([window]
    (full-screen? (default-screen-device) window)))

(defn- full-screen-ensure-undecorated
  "Windows should be undecorated when put into full-screen. If they're not
  this function strips their decorations and annotates them so when they're
  brought out of full-screen mode the decorations are restored."
  [window]
  (when (and window (not (config window :undecorated?)))
    (-> window
      dispose!
      (config! :undecorated? true)
      (put-meta! ::was-decorated? true)
      show!))
  window)

(defn- restore-full-screen-window-decorations
  "when full screen windows is moving back to normal, redecorate as needed."
  [^java.awt.GraphicsDevice device]
  (when-let [window (full-screen-window device)]
    (when (get-meta window ::was-decorated?)
      (-> window
        dispose!
        (config! :undecorated? false)
        (put-meta! ::was-decorated? nil)
        show!))))

(defn full-screen!
  "Make the given window/frame full-screen. Pass nil to return all windows
  to normal size."
  ([^java.awt.GraphicsDevice device window]
    (restore-full-screen-window-decorations device)
    (.setFullScreenWindow device (full-screen-ensure-undecorated (to-root window)))
    window)
  ([window]
    (full-screen! (default-screen-device) window)))

(defn toggle-full-screen!
  "Toggle the full-screen state of the given window/frame."
  ([^java.awt.GraphicsDevice device window]
    (full-screen! device
                  (if (full-screen? device window) nil window))
    window)
  ([window]
    (toggle-full-screen! (default-screen-device) window)))


(def ^{:private true} frame-on-close-map {
  :hide    JFrame/HIDE_ON_CLOSE
  :dispose JFrame/DISPOSE_ON_CLOSE
  :exit    JFrame/EXIT_ON_CLOSE
  :nothing JFrame/DO_NOTHING_ON_CLOSE
})

(defn- ^java.awt.Image frame-icon-converter [value]
  (cond
    (instance? java.awt.Image value) value
    :else (let [^javax.swing.ImageIcon i (make-icon value)]
            (.getImage i))))

(def frame-options
  (merge
    abstract-window-options
    (option-map
      (resource-option :resource [:title :icon])

      (bean-option
        [:on-close :default-close-operation] javax.swing.JFrame
        frame-on-close-map
        nil
        (keys frame-on-close-map))

      (bean-option
        [:menubar :j-menu-bar]
        javax.swing.JFrame
        nil nil
        "The frame's menu bar. See (seesaw.core/menubar).")

      (bean-option :title java.awt.Frame resource nil
                   ["The frame's title as string or resource key"])

      (bean-option :resizable? java.awt.Frame boolean)

      (bean-option :undecorated? java.awt.Frame boolean)

      (bean-option
        [:icon :icon-image]
        javax.swing.JFrame
        frame-icon-converter nil
        "The image to be displayed as the icon for this frame")

      (bean-option
        [:icons :icon-images]
        java.awt.Window
        (partial map frame-icon-converter) nil
        "Sequence of images to be displayed as the icon for this frame")

      (default-option
        :listen
        #(apply seesaw.event/listen %1 %2)
        nil
        ["vector of args for (seesaw.core/listen)"]))))

(option-provider javax.swing.JFrame frame-options)

(defn frame
  "Create a JFrame. Options:

    :id       id of the window, used by (select).

    :title    the title of the window

    :icon     the icon of the frame (varies by platform)

    :width    initial width. Note that calling (pack!) will negate this setting

    :height   initial height. Note that calling (pack!) will negate this setting

    :size     initial size. Note that calling (pack!) will negate this setting

    :minimum-size minimum size of frame, e.g. [640 :by 480]

    :content  passed through (make-widget) and used as the frame's content-pane

    :visible?  whether frame should be initially visible (default false)

    :resizable? whether the frame can be resized (default true)

    :on-close   default close behavior. One of :exit, :hide, :dispose, :nothing
                The default value is :hide. Note that the :window-closed event is
                only fired for values :exit and :dispose

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

  See:
    (seesaw.core/show!)
    (seesaw.core/hide!)
    (seesaw.core/move!)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JFrame.html
  "
  [& {:keys [width height visible? size]
      :as opts}]
  (cond-doto ^JFrame (apply-options (construct JFrame)
                                    (dissoc opts :width :height :visible?))
    (and (not size)
         (or width height)) (.setSize (or width 100) (or height 100))
    true       (.setLocationByPlatform true)
    visible?   (.setVisible (boolean visible?))))

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
      (let [^javax.swing.JPopupMenu w w]
      (if-let [p (.getParent w)]
        (get-root p)
        (get-root (.getInvoker w))))
    :else (get-root (.getParent ^java.awt.Component w))))

(defn to-root
  "Get the frame or window that contains the given widget. Useful for APIs
  like JDialog that want a JFrame, when all you have is a widget or event.
  Note that w is run through (to-widget) first, so you can pass event object
  directly to this."
  [w]
  (get-root (to-widget w)))

; I want to phase out to-frame. For now, it's an alias of to-root.
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

(def custom-dialog-options
  (merge
    frame-options
    (option-map
      (default-option :modal?
                #(.setModalityType ^java.awt.Dialog %1
                                  (or (dialog-modality-table %2)
                                      (dialog-modality-table (boolean %2)))))
      ; TODO This is a little odd.
      (default-option :parent #(.setLocationRelativeTo ^java.awt.Dialog %1 %2))

      ; These two override frame-options for purposes of type hinting and reflection
      (bean-option [:on-close :default-close-operation] javax.swing.JDialog frame-on-close-map)
      (bean-option [:content :content-pane] javax.swing.JDialog make-widget)
      (bean-option [:menubar :j-menu-bar] javax.swing.JDialog)

      ; Ditto here. Avoid reflection
      (bean-option :title java.awt.Dialog resource)
      (bean-option :resizable? java.awt.Dialog boolean))))

(option-provider java.awt.Dialog custom-dialog-options)

(def ^{:private true} dialog-result-property ::dialog-result)

(defn- is-modal-dialog? [dlg]
  (and (instance? java.awt.Dialog dlg)
       (not= (.getModalityType ^java.awt.Dialog dlg) java.awt.Dialog$ModalityType/MODELESS)))

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
    The dialog must be modal and created from within the DIALOG fn with
    :modal? set to true.

  See:
    (seesaw.core/dialog)
    (seesaw.core/custom-dialog)
  "
  [dlg result]
  ;(assert-ui-thread "return-from-dialog")
  (let [dlg         (to-root dlg)
        result-atom (get-meta dlg dialog-result-property)]
    (if result-atom
      (do
        (reset! result-atom result)
        (invoke-now (dispose! dlg)))
      (illegal-argument "Counld not find dialog meta data!"))))

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

  See:
    (seesaw.core/show!)
    (seesaw.core/return-from-dialog)
    http://download.oracle.com/javase/6/docs/api/javax/swing/JDialog.html
"
  [& {:keys [width height visible? modal? on-close size]
      :or {width 100 height 100 visible? false}
      :as opts}]
  (let [^JDialog dlg (apply-options
                       (construct JDialog)
                       (merge {:modal? true}
                              (dissoc opts :width :height :visible? :pack?)))]
    (when-not size (.setSize dlg width height))
    (.setLocationByPlatform dlg true)
    (if visible?
      (show! dlg)
      dlg)))


;*******************************************************************************
; Alert
(def ^{:private true} message-type-map {
  :error    JOptionPane/ERROR_MESSAGE
  :info     JOptionPane/INFORMATION_MESSAGE
  :warning  JOptionPane/WARNING_MESSAGE
  :question JOptionPane/QUESTION_MESSAGE
  :plain    JOptionPane/PLAIN_MESSAGE
})

(defn- alert-impl
  "
    showMessageDialog(Component parentComponent,
                      Object message,
                      String title,
                      int messageType,
                      Icon icon)
  "
  [source message {:keys [title type icon] :or {type :plain}}]
  (let [source (to-widget source)
        message (if (coll? message) (object-array message) (resource message))]
    (JOptionPane/showMessageDialog ^java.awt.Component source
                     message
                     (resource title)
                     (message-type-map type)
                     (make-icon icon))))

(defn alert
  "Show a simple message alert dialog:

    (alert [source] message & options)

  source  - optional parent component
  message - The message to show the user. May be a string, or list of strings, widgets, etc.
  options - additional options

  Additional options:

    :title The dialog title
    :type :warning, :error, :info, :plain, or :question
    :icon Icon to display (Icon, URL, etc)

  Examples:

    (alert \"Hello!\")
    (alert e \"Hello!\")

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JOptionPane.html#showMessageDialog%28java.awt.Component,%20java.lang.Object,%20java.lang.String,%20int%29
  "
  [& args]
  (let [n (count args)
        f (first args)
        s (second args)]
    (cond
      (or (= n 0) (keyword? f))
        (illegal-argument "alert requires at least one non-keyword arg")
      (= n 1) (alert-impl nil f {})
      (= n 2) (alert-impl f s {})
      (keyword? s) (alert-impl nil f (drop 1 args))
      :else (alert-impl f s (drop 2 args)))))

;*******************************************************************************
; Input
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
        message (if (coll? message) (object-array message) (resource message))
        choices (when choices (object-array (map #(InputChoice. % to-string) choices)))
        result  (JOptionPane/showInputDialog ^java.awt.Component source
                                 message
                                 (resource title)
                                 (message-type-map type)
                                 (make-icon icon)
                                 choices value)]
    (if (and result choices)
      (:value result)
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
        (illegal-argument "input requires at least one non-keyword arg")
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
  :content        "Please set the :content option."
  :option-type    :default
  :type           :plain
  :options        nil
  :default-option nil
  :success-fn     (fn [_] :success)
  :cancel-fn      (fn [_])
  :no-fn          (fn [_] :no)
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
                 It must be a seq of \"make-widget\"'able objects which will be
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
                 :option-type :ok-cancel
                 :success-fn (fn [p] (text (select (to-root p) [:#name]))))

  The dialog is not immediately shown. Use (seesaw.core/show!) to display the dialog.
  If the dialog is modal this will return the result of :success-fn, :cancel-fn or
  :no-fn depending on what button the user pressed.

  Alternatively if :options has been specified, returns the value which has been
  passed to (seesaw.core/return-from-dialog).

  See:
    (seesaw.core/show!)
    (seesaw.core/return-from-dialog)
"
  [& {:as opts}]
  ;; (Object message, int messageType, int optionType, Icon icon, Object[] options, Object initialValue)
  (let [{:keys [content option-type type
                options default-option success-fn cancel-fn no-fn]} (merge dialog-defaults opts)
        pane (JOptionPane.
              content
              (message-type-map type)
              (dialog-option-type-map option-type)
              nil                       ;icon
              (when options
                (into-array (map make-widget options)))
              (or default-option (first options))) ; default selection
        remaining-opts (apply dissoc opts :visible? (keys dialog-defaults))
        dlg            (apply custom-dialog :visible? false :content pane (reduce concat remaining-opts))]
      ;; when there was no options specified, default options will be
      ;; used, so the success-fn cancel-fn & no-fn must be called
      (when-not options
        (.addPropertyChangeListener pane JOptionPane/VALUE_PROPERTY
          (reify java.beans.PropertyChangeListener
            (propertyChange [this e]
              (let [v (.getNewValue e)
                    f (condp = v
                        JOptionPane/CLOSED_OPTION cancel-fn
                        JOptionPane/YES_OPTION    success-fn
                        JOptionPane/NO_OPTION     no-fn
                        JOptionPane/CANCEL_OPTION cancel-fn
                        cancel-fn)]
                (return-from-dialog e (f pane)))))))
      (if (:visible? opts)
        (show! dlg)
        dlg)))

;*******************************************************************************
; confirm
(defn- confirm-impl
  "
    showConfirmDialog(Component parentComponent,
                      Object message,
                      String title,
                      int optionType,
                      int messageType,
                      Icon icon)
  "
  [source message {:keys [title option-type type icon]
                   :or {type :plain option-type :ok-cancel}}]
  (let [source  (to-widget source)
        message (if (coll? message) (object-array message) (resource message))
        result  (JOptionPane/showConfirmDialog ^java.awt.Component source
                                 message
                                 (resource title)
                                 (dialog-option-type-map option-type)
                                 (message-type-map type)
                                 (make-icon icon))]
    (condp = result
      JOptionPane/NO_OPTION false
      JOptionPane/CANCEL_OPTION nil
      true)))

(defn confirm
  "Show a confirmation dialog:

    (confirm [source] message & options)

  source  - optional parent component
  message - The message to show the user. May be a string, or list of strings, widgets, etc.
  options - additional options

  Additional options:

    :title       The dialog title
    :option-type :yes-no, :yes-no-cancel, or :ok-cancel (default)
    :type        :warning, :error, :info, :plain, or :question
    :icon        Icon to display (Icon, URL, etc)

  Returns true if the user has hit Yes or OK, false if they hit No,
  and nil if they hit Cancel.

  See:
    http://docs.oracle.com/javase/6/docs/api/javax/swing/JOptionPane.html#showConfirmDialog%28java.awt.Component,%20java.lang.Object,%20java.lang.String,%20int,%20int%29
  "
  [& args]
  (let [n (count args)
        f (first args)
        s (second args)]
    (cond
      (or (= n 0) (keyword? f))
        (illegal-argument "confirm requires at least one non-keyword arg")
      (= n 1)      (confirm-impl nil f {})
      (= n 2)      (confirm-impl f s {})
      (keyword? s) (confirm-impl nil f (drop 1 args))
      :else        (confirm-impl f s (drop 2 args)))))


;*******************************************************************************
; Slider

(def slider-options
  (merge
    default-options
    (option-map
      model-option
      (bean-option :orientation javax.swing.JSlider orientation-table)
      (bean-option :value javax.swing.JSlider)
      (bean-option [:min :minimum] javax.swing.JSlider)
      (bean-option [:max :maximum] javax.swing.JSlider)
      (default-option :minor-tick-spacing
                      #(do (check-args (number? %2) ":minor-tick-spacing must be a number.")
                        (.setPaintTicks ^javax.swing.JSlider %1 true)
                        (.setMinorTickSpacing ^javax.swing.JSlider %1 %2)))
      (default-option :major-tick-spacing
                      #(do (check-args (number? %2) ":major-tick-spacing must be a number.")
                        (.setPaintTicks ^javax.swing.JSlider %1 true)
                        (.setMajorTickSpacing ^javax.swing.JSlider %1 %2)))
      (bean-option [:snap-to-ticks? :snap-to-ticks] javax.swing.JSlider boolean)
      (bean-option [:paint-ticks? :paint-ticks] javax.swing.JSlider boolean)
      (bean-option [:paint-labels? :paint-labels] javax.swing.JSlider boolean)
      (bean-option [:paint-track? :paint-track] javax.swing.JSlider boolean)
      (bean-option [:inverted? :inverted] javax.swing.JSlider boolean))))

(widget-option-provider javax.swing.JSlider slider-options)

(defn slider
  "Show a slider which can be used to modify a value.

      (slider ... options ...)

  Besides the default options, options can also be one of:

    :orientation   The orientation of the slider. One of :horizontal, :vertical.
    :value         The initial numerical value that is to be set.
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

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JSlider.html
"
  [& {:keys [orientation value min max minor-tick-spacing major-tick-spacing
             snap-to-ticks? paint-ticks? paint-labels? paint-track? inverted?]
      :as kw}]
  (let [sl (construct javax.swing.JSlider)]
    (apply-options sl kw)))


;*******************************************************************************
; Progress Bar
(def progress-bar-options
  (merge
    default-options
    (option-map
      model-option
      (bean-option :orientation javax.swing.JProgressBar orientation-table)
      (bean-option :value javax.swing.JProgressBar)
      (bean-option [:min :minimum] javax.swing.JProgressBar)
      (bean-option [:max :maximum] javax.swing.JProgressBar)
      (bean-option [:paint-string? :string-painted?] javax.swing.JProgressBar boolean)
      (bean-option :indeterminate? javax.swing.JProgressBar boolean))))

(widget-option-provider javax.swing.JProgressBar progress-bar-options)

(defn progress-bar
  "Show a progress-bar which can be used to display the progress of long running tasks.

      (progress-bar ... options ...)

  Besides the default options, options can also be one of:

    :orientation   The orientation of the progress-bar. One of :horizontal, :vertical. Default: :horizontal.
    :value         The initial numerical value that is to be set. Default: 0.
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

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JProgressBar.html

"
  [& {:keys [orientation value min max] :as opts}]
  (let [sl (construct javax.swing.JProgressBar)]
    (apply-options sl opts)))



;*******************************************************************************
; Selectors

; Implement getting and setting ids and classes for selectors
(def ^{:private true} id-property ::seesaw-widget-id)
(def ^{:private true} class-property ::seesaw-widget-class)

(extend-protocol seesaw.selector/Selectable
  javax.swing.JComponent
    (id-of* [this]
      (.getClientProperty this id-property))
    (id-of!* [this id]
      (.putClientProperty this id-property (keyword id)))
    (class-of* [this]
      (.getClientProperty this class-property))
    (class-of!* [this classes]
      (.putClientProperty this class-property
                          (set (map name (if (coll? classes) classes [classes])))))

  java.awt.Component
    (id-of* [this]
      (get-meta this id-property))
    (id-of!* [this id]
      (put-meta! this id-property (keyword id)))
    (class-of* [this]
      (get-meta this class-property))
    (class-of!* [this classes]
      (put-meta! this class-property
                        (set (map name (if (coll? classes) classes [classes]))))))

(defn select
  "Select a widget using the given selector expression. Selectors are *always*
   expressed as a vector. root is the root of the widget hierarchy to select
   from, usually either a (frame) or other container.

    (select root [:#id])          Look up widget by id. A single widget is
                                  always returned.

    (select root [:tag])          Look up widgets by \"tag\". In Seesaw tag is
                                  treated as the exact simple class name of a
                                  widget, so :JLabel would match both
                                  javax.swing.JLabel *and* com.me.JLabel.
                                  Be careful!

    (select root [:<class-name>]) Look up widgets by *fully-qualified* class name.
                                  Matches sub-classes as well. Always returns a
                                  sequence of widgets.

    (select root [:<class-name!>]) Same as above, but class must match exactly.

    (select root [:*])             Root and all the widgets under it

  Notes:
    This function will return a single widget *only* in the case where the selector
    is a single identifier, e.g. [:#my-id]. In *all* other cases, a sequence of
    widgets is returned. This is for convenience. Select-by-id is the common case
    where a single widget is almost always desired.

  Examples:

    To find a widget by id from an event handler, use (to-root) on the event to get
    the root and then select on the id:

      (fn [e]
        (let [my-widget (select (to-root e) [:#my-widget])]
          ...))

    Disable all JButtons (excluding subclasses) in a hierarchy:

      (config! (select root [:<javax.swing.JButton>]) :enabled? false)

    More:

      ; All JLabels, no sub-classes allowed
      (select root [:<javax.swing.JLabel!>])

      ; All JSliders that are descendants of a JPanel with id foo
      (select root [:JPanel#foo :JSlider])

      ; All JSliders (and sub-classes) that are immediate children of a JPanel with id foo
      (select root [:JPanel#foo :> :<javax.swing.JSlider>])

      ; All widgets with class foo. Set the class of a widget with the :class option
      (flow-panel :class :my-class) or (flow-panel :class #{:class1 :class2})
      (select root [:.my-class])
      (select root [:.class1.class2])

      ; Select all text components with class input
      (select root [:<javax.swing.text.JTextComponent>.input])

      ; Select all descendants of all panels with class container
      (select root [:JPanel.container :*])

  See:
    (seesaw.selector/select)
    https://github.com/cgrand/enlive
  "
  ([root selector]
    (check-args (vector? selector) "selector must be vector")
    (let [root (to-widget root)
          result (seesaw.selector/select root selector)
          id? (and (nil? (second selector)) (seesaw.selector/id-selector? (first selector)))]
      (if id? (first result) result))))

(defrecord ^{:private true} SelectWith [widget]
  clojure.lang.IFn
  (invoke [this selector]
    (select widget selector))
  ToWidget
  (to-widget* [this] widget))

(defn select-with
  "Returns an object with the following properties:

   * Equivalent to (partial seesaw.core/select (to-widget target)), i.e. it
     returns a function that performs a select on the target.
   * Calling (to-widget) on it returns the same value as (to-widget target).

  This basically allows you to pack a widget and the select function into a single
  package for convenience. For example:

    (defn make-frame [] (frame ...))

    (defn add-behaviors [$]
      (let [widget-a ($ [:#widget-a])
            buttons  ($ [:.button])
            ...]
        ...)
      $)

    (defn -main []
      (-> (make-frame) select-with add-behaviors pack! show!))

  See:
    (seesaw.core/select)
    (seesaw.core/to-widget)
  "
  [target]
  (SelectWith. (to-widget target)))

(defn group-by-id
  "Group the widgets in a hierarchy starting at some root into a map
  keyed by :id. Widgets with no id are ignored. If an id appears twice,
  the 'later' widget wins.

    root is any (to-widget)-able object.

  Examples:

    Suppose you have a form with with widgets with ids :name, :address,
    :phone, :city, :state, :zip.
    You'd like to quickly grab all those widgets and do something with
    them from an event handler:

      (fn [event]
        (let [{:keys [name address phone city state zip]} (group-by-id event)
          ... do something ...))

    This is functionally equivalent to, but faster than:

      (let [name (select event [:#name])
            address (select event [:#address])
            phone (select event [:#phone])
            ... and so on ...]
          ... do something ...)

  See:
    (seesaw.core/select)
  "
  [root]
  (reduce
    (fn [m c]
      (if-let [id (id-of c)]
        (assoc m id c)
        m))
    {}
    (select (to-widget root) [:*])))

(defmacro with-widgets
  "Macro to ease construction of multiple widgets. The first argument
  is a vector of widget constructor forms, each with an :id option.
  The name of the value of each :id is used to generate a binding in
  the scope of the macro.

  Examples:

    (with-widgets [(label :id :foo :text \"foo\")
                   (button :id :bar :text \"bar\")]
       ...)

    ; is equivalent to
    (let [foo (label :id :foo :text \"foo\")
          bar (button :id :bar :text \"bar\")]
       ...)

  Notes:

  If you're looking for something like this to reduce boilerplate with
  selectors on multiple widgets, see (seesaw.core/group-by-id).

  See:
    (seesaw.core/group-by-id)
  "
  [widgets & body]
  `(let [~@(mapcat (fn [[f & args :as widget]]
                      (if-let [id (:id (apply hash-map args))]
                        [(symbol (name id)) widget]
                        (illegal-argument "No :id specified for widget in %s"
                              (pr-str widget))))
             widgets)]
     ~@body))

;*******************************************************************************
; Widget layout manipulation

(defn add!
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
  [container subject & more]
  (layout/handle-structure-change
    (apply layout/add!-impl container subject more)))

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
  (layout/handle-structure-change
    (apply layout/remove!-impl container subject more)))

(defn replace!
  "Replace old-widget with new-widget from container. container and old-widget
  are passed through (to-widget). new-widget is passed through make-widget.
  Note that the layout constraints of old-widget are retained for the new widget.
  This is different from the behavior you'd get with just remove/add in Swing.

  The container is properly revalidated and repainted after replacement.

  Examples:

    ; Replace a label with a new label.
    (def lbl (label \"HI\"))
    (def p (border-panel :north lbl))
    (replace! p lbl \"Goodbye\")

  Returns the target container *after* it's been passed through (to-widget).
  "
  [container old-widget new-widget]
  (layout/handle-structure-change
    (layout/replace!-impl
      (to-widget container)
      (to-widget old-widget)
      (make-widget new-widget))))


;*******************************************************************************
; Widget "value"

(defn value
  "Return the 'value' of a widget. target is passed through (to-widget) as usual.

  Basically, there are two possibilities:

    * It's a container: A map of widget values keyed by :id is built
        recursively from all its children.
    * The 'natural' value for the widget is returned, usually the text,
      or the current selection of the widget.

  See:
    (seesaw.core/value!)
    (seesaw.core/selection)
    (seesaw.core/group-by-id)

  This idea is shamelessly borrowed from Clarity https://github.com/stathissideris/clarity
  "
  [target]
  (seesaw.value/value* (or (to-widget target) target)))

(defn value!
  "Set the 'value' of a widget. This is the dual of (seesaw.core/value). target
  is passed through (to-widget) as usual.

  Basically, there are two possibilities:

    * target is a container: The value is a map of widget values keyed by :id. These
        values are applied to all descendants of target.
    * otherwise, v is a new 'natural' value for the widget, usually the text,
      or the current selection of the widget.

  In either case (to-widget target) is returned.

  Examples:

    Imagine there's widget :foo, :bar, :yum in frame f:

      (value! f {:foo \"new foo text\" :bar 99 :yum \"new yum text\"})

  See:
    (seesaw.core/value)
    (seesaw.core/selection)
    (seesaw.core/group-by-id)

  This idea is shamelessly borrowed from Clarity https://github.com/stathissideris/clarity
  "
  [target v]
  (seesaw.value/value!* (or (to-widget target) target) v))
