;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.core
  (:use [seesaw util font border color])
  (:require [seesaw.event :as sse]
            [seesaw.timer :as sst]
            [seesaw.selection :as sss]
            [seesaw.icon :as ssi]
            [seesaw.action :as ssa]
            [seesaw.table :as ss-table]
            [seesaw.cells :as cells]
            [seesaw.graphics :as ssg])
  (:import [java.util EventObject]
           [javax.swing 
             SwingUtilities SwingConstants UIManager
             Action
             BoxLayout
             JFrame JComponent Box JPanel JScrollPane JSplitPane JToolBar JTabbedPane
             JLabel JTextField JTextArea 
             AbstractButton JButton JToggleButton JCheckBox JRadioButton
             JOptionPane]
           [javax.swing.text JTextComponent]
           [java.awt Component FlowLayout BorderLayout GridLayout 
              GridBagLayout GridBagConstraints
              Dimension]))

(declare to-widget)
(declare popup-option-handler)

;(set! *warn-on-reflection* true)
(defn invoke-later* [f] (SwingUtilities/invokeLater f))

(defn invoke-now* [f] 
  (if (SwingUtilities/isEventDispatchThread)
    (f)
    (SwingUtilities/invokeAndWait f)))

(defmacro invoke-later 
  "Equivalent to SwingUtilities/invokeLater. Executes the given body sometime
  in the future on the Swing UI thread. For example,

    (invoke-later
      (config! my-label :text \"New Text\"))

  See http://download.oracle.com/javase/6/docs/api/javax/swing/SwingUtilities.html#invokeLater(java.lang.Runnable) 
  "
  [& body] `(invoke-later* (fn [] ~@body)))

(defmacro invoke-now   
  "Equivalent to SwingUtilities/invokeAndWait. Executes the given body immediately
  on the Swing UI thread, possibly blocking the current thread if it's not the Swing
  UI thread. For example,

    (invoke-now
      (config! my-label :text \"New Text\"))

  Be very careful with this function in the presence of locks and stuff.

  See http://download.oracle.com/javase/6/docs/api/javax/swing/SwingUtilities.html#invokeAndWait(java.lang.Runnable) 
  "
  [& body] `(invoke-now*   (fn [] ~@body)))

(defn native!
  "Set native look and feel and other options to try to make things look right.
  This function must be called very early, like before any other Seesaw or Swing
  calls!"
  []
  (System/setProperty "apple.laf.useScreenMenuBar" "true")
  (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName)))

; alias timer/timer for convenience
(def timer sst/timer)

; alias event/listen for convenience
(def listen sse/listen)

; alias action/action for convenience
(def action ssa/action)

;(def choose-file seesaw.chooser/choose-file)

; to-widget wrapper and stuff for (seesaw.selection/selection)
(defn selection 
  "Gets the selection of a widget. target is passed through (to-widget)
  so event objects can also be used. The default behavior is to return
  a *single* selection value, even if the widget supports multiple selection.
  If there is no selection, returns nil.

  options is an option map which supports the following flags:

    multi? - If true the return value is a seq of selected values rather than
      a single value.
  
  See also seesaw.selection/selection.
  "
  ([target] (selection target {}))
  ([target options] (sss/selection (to-widget target) options)))

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
  ([target opts new-selection] (sss/selection! (to-widget target) opts new-selection)))

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
  (if-let [u (to-url v)] 
    (JLabel. (make-icon u)) 
    (JLabel. (str v))))

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
    A URL -> a label with the image located at the url
    A non-url string -> a label with the given text

   If create? is false, will return nil for all rules (see above) that
   would create a new widget. The default value for create? is false
   to avoid inadvertently creating widgets all over the place.
  "
  ([v]         (to-widget v false))
  ([v create?] (when v (to-widget* v create?))))

;*******************************************************************************
; Generic widget stuff

(defn repaint!
  "Request a repaint of a list of widget-able things.

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
  
(defn- add-widget 
  ([c w] (add-widget c w nil))
  ([c w constraint] 
    (let [w* (to-widget w true)]
      (.add c w* constraint)
      w*)))

(defn- add-widgets
  [c ws]
  (doseq [w ws]
    (add-widget c w))
  (doto c
    .revalidate
    .repaint))

(def ^{:private true} id-property "seesaw-widget-id")

(defn id-for 
  "Returns the id of the given widget if the :id property was specified at
   creation. See also (select)."
  [w] 
  (when (instance? javax.swing.JComponent w) (.getClientProperty w id-property)))

(def ^{:private true} h-alignment-table 
  (constant-map SwingConstants :left :right :leading :trailing :center ))

(def ^{:private true} v-alignment-table
  (constant-map SwingConstants :top :center :bottom))

(def ^{:private true} orientation-table
  (constant-map SwingConstants :horizontal :vertical))

(defn- id-option-handler [w id]
  (let [id-key (name id)]
    (cond
      (instance? JComponent w)
        (let [existing-id (.getClientProperty w id-property)]
          (when existing-id (throw (IllegalStateException. (str ":id is already set to " existing-id))))
          ; TODO should we enforce unique ids?
          (.putClientProperty w id-property id-key)))
      ; TODO need to figure out how to store JFrame ids. JFrame/getFrames is pretty useless
  ))

(defn- location-option-handler [w v]
  (cond
    ; TODO to-point protocol
    (instance? java.awt.Point v) (.setLocation w v)
    :else (.setLocation w (first v) (second v))))

(defn- bounds-option-handler [w v]
  (cond
    ; TODO to-rect protocol?
    (instance? java.awt.Rectangle v) (.setBounds w v)
    :else (.setBounds w (nth v 0) (nth v 1) (nth v 2) (nth v 3))))

(def ^{:private true} default-options {
  :id          id-option-handler
  :listen      #(apply sse/listen %1 %2)
  :opaque?     #(.setOpaque %1 (boolean %2))
  :enabled?    #(.setEnabled %1 (boolean %2))
  :background  #(.setBackground %1 (to-color %2))
  :foreground  #(.setForeground %1 (to-color %2))
  :border      #(.setBorder %1 (to-border %2))
  :font        #(.setFont %1 (to-font %2))
  :tip         #(.setToolTipText %1 (str %2))
  :text        #(.setText %1 (str %2))
  :icon        #(.setIcon %1 (make-icon %2))
  :action      #(.setAction %1 %2)
  :editable?   #(.setEditable %1 (boolean %2))
  :halign      #(.setHorizontalAlignment %1 (h-alignment-table %2))
  :valign      #(.setVerticalAlignment %1 (v-alignment-table %2)) 
  :orientation #(.setOrientation %1 (orientation-table %2))
  :items       #(add-widgets %1 %2)
  :model       #(.setModel %1 %2)
  :preferred-size #(.setPreferredSize %1 (to-dimension %2))
  :minimum-size   #(.setMinimumSize %1 (to-dimension %2))
  :maximum-size   #(.setMaximumSize %1 (to-dimension %2))
  :size           #(let [d (to-dimension %2)]
                     (doto %1 
                       (.setPreferredSize d)
                       (.setMinimumSize d)
                       (.setMaximumSize d)))
  :location   location-option-handler
  :bounds     bounds-option-handler
  :popup      #(popup-option-handler %1 %2)
})

(defn apply-default-opts
  "only used in tests!"
  ([p] (apply-default-opts p {}))
  ([^javax.swing.JComponent p {:as opts}]
    (apply-options p opts default-options)))

;*******************************************************************************
; Widget configuration stuff

(defprotocol ConfigureWidget (config* [target args]))

(extend-type java.util.EventObject ConfigureWidget 
  (config* [target args] (config* (to-widget target false) args)))

(extend-type javax.swing.JComponent ConfigureWidget 
  (config* [target args] 
    (reapply-options target args default-options)))

(extend-type Action ConfigureWidget 
  (config* [target args] 
    (reapply-options target args default-options)))

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
; Border Layout

(def ^{:private true}  border-layout-dirs 
  (constant-map BorderLayout :north :south :east :west :center))

(def ^{:private true} border-layout-options 
  (merge
    { :hgap #(.setHgap (.getLayout %1) %2)
      :vgap #(.setVgap (.getLayout %1) %2) }
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
  
  See http://download.oracle.com/javase/6/docs/api/java/awt/BorderLayout.html
  "
  [& opts]
  (let [p (JPanel. (BorderLayout.))]
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
  (let [p (JPanel. (FlowLayout.))]
    (apply-options p opts (merge default-options flow-panel-options))))

;*******************************************************************************
; Boxes

(def ^{:private true} box-layout-dir-table {
  :horizontal BoxLayout/X_AXIS 
  :vertical   BoxLayout/Y_AXIS 
})

(defn box-panel
  [dir & opts]
  (let [panel  (JPanel.)
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
        panel    (JPanel. layout)]
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
  (doseq [[widget constraints] (realize-grid-bag-constraints items)]
    (when widget
      (add-widget panel widget constraints))))

(def ^{:private true} form-panel-options {
  :items add-grid-bag-items
})

(defn form-panel
  "A panel that uses a GridBagLayout. Also aliased as (grid-bag-panel) if you
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
  (let [^java.awt.Container p (JPanel. (GridBagLayout.))]
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
  (doseq [[widget constraint] items]
    (add-widget parent widget constraint)))

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
  (let [p (JPanel. (net.miginfocom.swing.MigLayout.))]
    (apply-options p opts (merge default-options mig-panel-options))))

;*******************************************************************************
; Labels

(defn label 
  "Create a label. Supports all default properties. Can take two forms:

      (label \"My Label\")  ; Single text argument for the label

  or with full options:

      (label :id :my-label :text \"My Label\" ...)

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JLabel.html
  "
  [& args]
  (if (next args)
    (apply-options (JLabel.) args default-options)
    (apply label :text args)))


;*******************************************************************************
; Buttons

(def ^{:private true} button-options {
  :selected?   #(.setSelected %1 (boolean %2))
})

(defn- apply-button-defaults
  ([button args] (apply-button-defaults button args {}))
  ([button args custom-options]
    (apply-options button args (merge default-options button-options custom-options))))

(defn button   [& args] (apply-button-defaults (JButton.) args))
(defn toggle   [& args] (apply-button-defaults (JToggleButton.) args))
(defn checkbox [& args] (apply-button-defaults (JCheckBox.) args))
(defn radio    [& args] (apply-button-defaults (JRadioButton.) args))

;*******************************************************************************
; Text widgets
(def ^{:private true} text-options {
  ; TODO split into single/multi options since some of these will fail if
  ; multi-line? is false  
  :columns     #(.setColumns %1 %2) 
  :rows        #(.setRows    %1 %2)
  :wrap-lines? #(.setLineWrap %1 (boolean %2))
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
                  t (if multi-line? (JTextArea.) (JTextField.))]
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

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JEditorPane.html"
  [& opts]
  (apply-options (javax.swing.JEditorPane.) opts (merge default-options text-options)))

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

  Note that retrieving and setting the current selection of the list box is fully
  supported by the (selection) and (selection!) functions.

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JList.html 
  "
  [& args]
  (apply-options (javax.swing.JList.) args (merge default-options listbox-options)))

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

  See:
    seesaw.table/table-model 
    seesaw.examples.table
    http://download.oracle.com/javase/6/docs/api/javax/swing/JTable.html"
  [& args]
  (apply-options 
    (doto (javax.swing.JTable.)
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

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JTree.html
  "
  [& args]
  (apply-options (javax.swing.JTree.) args (merge default-options tree-options)))

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

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JComboBox.html
  "
  [& args]
  (apply-options (javax.swing.JComboBox.) args (merge default-options combobox-options)))

;*******************************************************************************
; Scrolling

(defn scrollable 
  "Wrap target in a JScrollPane and return the scroll pane.
  
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JScrollPane.html
  "
  [target]
  (let [sp (JScrollPane. target)]
    sp))

;*******************************************************************************
; Splitter
(defn splitter
  [dir left right & opts]
  (JSplitPane. (dir {:left-right JSplitPane/HORIZONTAL_SPLIT
                     :top-bottom JSplitPane/VERTICAL_SPLIT})
               (to-widget left true)
               (to-widget right true)))

(defn left-right-split 
  "Create a left/right (horizontal) splitpane with the given widgets.
  
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JSplitPane.html
  "
  [left right & args] (apply splitter :left-right left right args))

(defn top-bottom-split 
  "Create a top/bottom (vertical) split pane with the given widgets
  
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JSplitPane.html
  "
  [top bottom & args] (apply splitter :top-bottom top bottom args))

;*******************************************************************************
; Separator

(defn separator
  "Create a separator.

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JSeparator.html
  "
  [& opts]
  (apply-options (javax.swing.JSeparator.) opts default-options))

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
  
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JMenu.html"
  [& opts]
  (apply-button-defaults (javax.swing.JMenu.) opts menu-options))

(defn popup 
  "Create a new popup menu. Additional options:

    :items Sequence of menu item-like things (actions, icons, JMenuItems, etc)

  Note that in many cases, the :popup option is what you want if you want to
  show a context menu on a widget. It handles all the yucky mouse stuff and
  fixes various eccentricities of Swing.
  
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JPopupMenu.html"
  [& opts]
  (apply-options (javax.swing.JPopupMenu.) opts (merge default-options menu-options)))


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
  
  See seesaw.core/frame
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JMenuBar.html
  "
  [& opts]
  (apply-options (javax.swing.JMenuBar.) opts default-options))

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

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JToolBar.html
  "
  [& opts]
  (apply-options (JToolBar.) opts (merge default-options toolbar-options)))

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

    :title     Title of the tab
    :tip       Tab's tooltip text
    :icon      Tab's icon, passed through (icon)
    :content   The content of the tab, passed through (to-widget) as usual.

  Returns the new JTabbedPane.

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JToolBar.html
  "
  [& opts]
  (apply-options (JTabbedPane.) opts (merge default-options tabbed-panel-options)))

;*******************************************************************************
; Canvas

(def ^{:private true} paint-property "seesaw-paint")

(defn- canvas-paint-option-handler [c v]
  (cond 
    (nil? v) (canvas-paint-option-handler c {:before nil :after nil :super? true})
    (fn? v)  (canvas-paint-option-handler c {:after v})
    (map? v) (do (.putClientProperty c paint-property v) (.repaint c))
    :else (throw (IllegalArgumentException. "Expect map or function for :paint property"))))

(def ^{:private true} canvas-options {
  :paint canvas-paint-option-handler
})

(defn- create-paintable []
  (proxy [javax.swing.JPanel] []
    (paintComponent [g]
      (let [{:keys [before after super?] :or {super? true}} (.getClientProperty this paint-property)]
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

  See http://download.oracle.com/javase/6/docs/api/javax/swing/JComponent.html#paintComponent%28java.awt.Graphics%29 
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
})

(defn frame
  "Create a JFrame. Options:

    :id       id of the window, used by (select).
    :title    the title of the window
    :pack?     true/false whether JFrame/pack should be called (default true)
    :width    initial width if :pack? is false
    :height   initial height if :pack? is false
    :size     initial size if :pack? is false, e.g. [640 :by 480]
    :minimum-size minimum size of frame, e.g. [640 :by 480]
    :content  passed through (to-widget) and used as the frame's content-pane
    :visible?  whether frame should be initially visible (default true)
    :resizable? whether the frame can be resized (default true)
    :on-close   default close behavior. One of :exit, :hide, :dispose, :nothing

  returns the new frame.
 
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JFrame.html 
  "
  [& {:keys [width height visible? pack?] 
      :or {width 100 height 100 visible? true pack? true}
      :as opts}]
  (cond-doto (apply-options (JFrame.) 
               (dissoc opts :width :height :visible? :pack?) frame-options)
    true     (.setSize width height)
    true     (.setVisible (boolean visible?))
    pack?    (.pack)))

(defn- get-root
  "Basically the same as SwingUtilities/getRoot, except handles JPopupMenus 
  by following the invoker of the popup if it doesn't have a parent. This
  allows (to-frame) to work correctly on action event objects fired from
  menus.
  
  Returns top-level Window (e.g. a JFrame), or nil if not found."
  [w]
  (cond
    (nil? w) w
    (instance? java.awt.Window w) w
    (instance? javax.swing.JPopupMenu w) 
      (if-let [p (.getParent w)] 
        (get-root p) 
        (get-root (.getInvoker w)))
    :else (get-root (.getParent w))))

(defn to-frame 
  "Get the frame or window that contains the given widget. Useful for APIs
  like JDialog that want a JFrame, when all you have is a widget or event.
  Note that w is run through (to-widget) first, so you can pass event object
  directly to this."
  [w]
  (get-root (to-widget w)))
  ;(SwingUtilities/getRoot (to-widget w)))


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
      (= n 1)      (input-impl nil f)
      (= n 2)      (input-impl f s)
      (keyword? s) (input-impl nil f (drop 1 args))
      :else        (input-impl f  s (drop 2 args)))))

;*******************************************************************************
; Selectors
(def ^{:private true} id-regex #"^#(.+)$")

(defn select
  "Select a widget using the given selector expression. Selectors are *always*
   expressed as a vector. root is the root of the widget hierarchy to select
   from, usually either a (frame) or other container.

    (select root [:#id])   Look up widget by id. A single widget is returned
    (select root [:*])     root and all the widgets under it

   For example, to find a widget by id from an event handler, use (to-frame) on
   the event to get the root:

    (fn [e]
      (let [my-widget (select (to-frame e) [:#my-widget])]
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

