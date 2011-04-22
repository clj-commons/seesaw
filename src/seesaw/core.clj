;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.core
  (:use seesaw.util)
  (:use seesaw.font)
  (:use seesaw.border)
  (:use seesaw.color)
  (:require [seesaw.event :as sse]
            [seesaw.selection :as sss])
  (:import [java.util EventObject]
           [javax.swing 
             SwingUtilities SwingConstants 
             Icon Action AbstractAction ImageIcon
             BoxLayout
             JFrame JComponent Box JPanel JScrollPane JSplitPane JToolBar JTabbedPane
             JLabel JTextField JTextArea 
             AbstractButton JButton JToggleButton JCheckBox JRadioButton
             JOptionPane]
           [javax.swing.text JTextComponent]
           [javax.swing.event ChangeListener DocumentListener]
           [java.awt Component FlowLayout BorderLayout GridLayout GridBagLayout GridBagConstraints
                     Dimension ItemSelectable Image]
           [java.awt.event MouseAdapter ActionListener]))

(declare to-widget)

;(set! *warn-on-reflection* true)
(defn invoke-later [f] (SwingUtilities/invokeLater f))
(defn invoke-now [f] (SwingUtilities/invokeAndWait f))

; alias event/add-listener for convenience
(def listen sse/add-listener)

; to-widget wrapper and stuff for (seesaw.selection/selection)
(defn selection 
  "Gets/sets the selection on a widget. target is passed through (to-widget)
  so event objects can also be used.
  
  If called with a single widget argument returns a seq containing the current
  selection, or nil if there is no selection. Use (first) for single-selection
  widgets like checkboxes.

  If called with an additional argument, sets the current selection. The
  interpretation of the argument depends on the type of widget:

    JCheckBox, JToggleButton, etc: truthy value sets checkmark, etc.
    JList: argument is a list of values to select, or nil to clear selection
    JComboBox: argument is the value to select

  Returns the target.

  See also seesaw.selection/selection.
  "
  [target & args]
  (apply sss/selection (to-widget target) args))

;*******************************************************************************
; Icons

(defn icon [p]
  (cond
    (nil? p) nil 
    (instance? javax.swing.Icon p) p
    (instance? java.awt.Image p) (ImageIcon. p)
    (instance? java.net.URL p) (ImageIcon. p)
    :else  (ImageIcon. (to-url p))))

(def ^{:private true} make-icon icon)

;*******************************************************************************
; Actions

(defn action [f & {:keys [name tip icon] :or { name "" }}]
  (doto (proxy [AbstractAction] [] (actionPerformed [e] (f e)))
    (.putValue Action/NAME (str name))
    (.putValue Action/SHORT_DESCRIPTION tip)
    (.putValue Action/SMALL_ICON (make-icon icon))))

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

(def ^{:private true} widget-by-id (atom {}))

(defn id-for 
  "Returns the id of the given widget if the :id property was specified at
   creation. See also (select)."
  [^javax.swing.JComponent w] (.getClientProperty w id-property))

(def ^{:private true} h-alignment-table 
  (constant-map SwingConstants :left :right :leading :trailing :center ))

(def ^{:private true} v-alignment-table
  (constant-map SwingConstants :top :center :bottom))

(def ^{:private true} orientation-table
  (constant-map SwingConstants :horizontal :vertical))

(defn- id-option-handler [w id]
  (let [id-key (name id)
        existing-id (.getClientProperty w id-property)]
    (when existing-id (throw (IllegalStateException. (str ":id is already set to " existing-id))))
    ; TODO should we enforce unique ids?
    (.putClientProperty w id-property id-key)
    (swap! widget-by-id assoc id-key w)))

(def ^{:private true} default-options {
  :id          id-option-handler
  :listen      #(apply sse/add-listener %1 %2)
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
})

(def ^{:private true} options-property "seesaw-creation-options")

(defn apply-options
  [target opts handler-map]
  (check-args (or (map? opts) (even? (count opts))) 
              "opts must be a map or have an even number of entries")
  (doseq [[k v] (if (map? opts) opts (partition 2 opts))]
    (if-let [f (get handler-map k)]
      (f target v)
      (throw (IllegalArgumentException. (str "Unknown option " k)))))
  (cond-doto target
    (instance? JComponent target) (.putClientProperty options-property handler-map)))

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
    (let [options (or (.getClientProperty target options-property) default-options)]
      (apply-options target args options))))

(defn config
  "Applies properties in the argument list to one or more targets. For example:

    (config button1 :enabled? false :text \"I' disabled\")

  or:

    (config [button1 button2] :enabled? false :text \"We're disabled\")
 
  Targets may be actual widgets, or convertible to widgets with (to-widget).
  For example, the target can be an event object.

  Returns the input targets."
  [targets & args]
  (doseq [target (to-seq targets)]
    (config* target args))
  targets)


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
  :align-on-baseline #(.setAlignOnBaseline (.getLayout %1) (boolean %2))
})

(defn flow-panel
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

(defn horizontal-panel [& opts] (apply box-panel :horizontal opts))
(defn vertical-panel   [& opts] (apply box-panel :vertical opts))

;*******************************************************************************
; Grid

(def ^{:private true} grid-panel-options {
  :hgap #(.setHgap (.getLayout %1) %2)
  :vgap #(.setVgap (.getLayout %1) %2)
})

(defn grid-panel
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

  This creates a label/field pair where the field expands."
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

  See http://www.miglayout.com.
  "
  [& opts]
  (let [p (JPanel. (net.miginfocom.swing.MigLayout.))]
    (apply-options p opts (merge default-options mig-panel-options))))

;*******************************************************************************
; Labels

(defn label 
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
  [button args]
  (apply-options button args (merge default-options button-options)))

(defn button   [& args] (apply-button-defaults (JButton.) args))
(defn toggle   [& args] (apply-button-defaults (JToggleButton.) args))
(defn checkbox [& args] (apply-button-defaults (JCheckBox.) args))
(defn radio    [& args] (apply-button-defaults (JRadioButton.) args))

;*******************************************************************************
; Text widgets
(def ^{:private true} text-options {
  :columns #(.setColumns %1 %2) })

(defn text
  "Create a text field or area. Given a single argument, creates a JTextField 
  using the argument as the initial text value. Otherwise, supports the 
  following properties:

    :text         Initial text content
    :multi-line?  If true, a JTextArea is created (default false)
    :editable?    If false, the text is read-only (default true)

  To listen for document changes, use the :listen option:

    (text :listen [:document #(... handler ...)])

  or attach a listener later with (listen):
    
    (text :id :my-text ...)
        ...
    (listen (select :#my-text) :document #(... handler ...))
  " 
  [& args]
  (let [n (count args)
        one? (= n 1)
        two? (= n 2)
        [arg0 arg1] args
        widget? (or (instance? JTextComponent arg0) (instance? AbstractButton arg0))
        multi? (or (coll? arg0) (seq? arg0))]
    ; TODO this is crying out for a multi-method or protocol
    (cond
      (and one? widget?)  (.getText arg0)
      (and one? multi?)   (map #(.getText %) arg0)
      one?                (text :text arg0)
      (and two? widget?)  (doto arg0 (.setText arg1))
      (and two? multi?)   (do (doseq [w arg0] (.setText w arg1)) arg0)

      :else (let [{:keys [multi-line?] :as opts} args
                  t (if multi-line? (JTextArea.) (JTextField.))]
            (apply-options t 
              (dissoc opts :multi-line?)
              (merge default-options text-options))))))

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
  :model (fn [lb m] ((:model default-options) lb (to-list-model m)))
})

(defn listbox
  [& args]
  (apply-options (javax.swing.JList.) args (merge default-options listbox-options)))

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
  :model (fn [lb m] ((:model default-options) lb (to-combobox-model m)))
})

(defn combobox
  [& args]
  (apply-options (javax.swing.JComboBox.) args (merge default-options combobox-options)))

;*******************************************************************************
; Scrolling

(defn scrollable 
  "Wrap target in a JScrollPane and return the scroll pane"
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
  "Create a left/right (horizontal) splitpane with the given widgets"
  [left right & args] (apply splitter :left-right left right args))

(defn top-bottom-split 
  "Create a top/bottom (vertical) split pane with the given widgets"
  [top bottom & args] (apply splitter :top-bottom top bottom args))

;*******************************************************************************
; Separator

(defn separator
  [& opts]
  (apply-options (javax.swing.JSeparator.) opts default-options))

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
  "
  [& opts]
  (apply-options (JTabbedPane.) opts (merge default-options tabbed-panel-options)))

;*******************************************************************************
; Frame
(def ^{:private true} frame-options {
  :title      #(.setTitle %1 (str %2))
  :resizable? #(.setResizable %1 (boolean %2))
  :content    #(.setContentPane %1 (to-widget %2 true))
})

(defn frame
  "Create a JFrame. Options:

    :title    the title of the window
    :pack?     true/false whether JFrame/pack should be called (default true)
    :width    initial width if :pack is false
    :height   initial height if :pack is true
    :content  passed through (to-widget) and used as the frame's content-pane
    :visible?  whether frame should be initially visible (default true)
    :resizable? whether the frame can be resized (default true)

  returns the new frame."

  [& {:keys [width height visible? pack?] 
      :or {width 100 height 100 visible? true pack? true}
      :as opts}]
  (cond-doto (apply-options (JFrame.) 
               (dissoc opts :width :height :visible? :pack?) frame-options)
    true     (.setSize width height)
    true     (.setVisible (boolean visible?))
    pack?    (.pack)))

(defn to-frame 
  "Get the frame or window that contains the given widget. Useful for APIs
  like JDialog that want a JFrame, when all you have is a widget or event.
  Note that w is run through (to-widget) first, so you can pass event object
  directly to this."
  [w]
  (SwingUtilities/getRoot (to-widget w)))

;*******************************************************************************
; Alert
(defn alert
  ([source message] 
    (JOptionPane/showMessageDialog (to-widget source) (str message)))
  ([message] (alert nil message)))


;*******************************************************************************
; Selectors
(def ^{:private true} id-regex #"^#(.+)$")

(defn select
  "Select a widget using the given selector expression:

    :#id    Look up widget by id. A single widget is returned

   Someday more selectors will be supported :)
  "
  ([selector]
    (if-let [[_ id] (re-find id-regex (name selector))]
      (get @widget-by-id id))))

