(ns seesaw.core
  (:use seesaw.util)
  (:use seesaw.font)
  (:use seesaw.border)
  (:use seesaw.color)
  (:require [seesaw.event :as sse])
  (:import [javax.swing 
             SwingUtilities SwingConstants 
             Icon Action AbstractAction ImageIcon
             BoxLayout
             JFrame JComponent Box JPanel JScrollPane JSplitPane JToolBar JTabbedPane
             JLabel JTextField JTextArea 
             AbstractButton JButton JToggleButton JCheckBox JRadioButton
             JOptionPane]
           [javax.swing.event ChangeListener DocumentListener]
           [java.awt Component FlowLayout BorderLayout GridLayout Dimension ItemSelectable Image]
           [java.awt.event MouseAdapter ActionListener]))
;(set! *warn-on-reflection* true)
(defn invoke-later [f] (SwingUtilities/invokeLater f))
(defn invoke-now [f] (SwingUtilities/invokeAndWait f))

; alias event/add-listener for convenience
(def listen sse/add-listener)

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
    :fill-h -> Box/createHorizontalGlue
    :fill-v -> Box/createVerticalGlue
    [:fill-h n] -> Box/createHorizontalStrut with width n
    [:fill-v n] -> Box/createVerticalStrut with height n
    [width :by height] -> create rigid area with given dimensions
    A URL -> a label with the image located at the url
    A non-url string -> a label with the given text

   If create? is false, will return nil for all rules (see above) that
   would create a new widget.
  "
  ([v] (to-widget v true))
  ([v create?]
    (when v (to-widget* v create?))))

;*******************************************************************************
; Generic widget stuff

(defn- add-widget 
  ([c w] (add-widget c w nil))
  ([c w constraint] 
   (let [w* (to-widget w)]
    (.add c w* constraint)
    w*)))

(defn- add-widgets
  [c ws]
  (doseq [w ws]
    (add-widget c w))
  c)

(def ^{:private true} id-property "seesaw-widget-id")

(def ^{:private true} widget-by-id (atom {}))

(defn id-for 
  "Returns the id of the given widget if the :id property was specified at
   creation. See also (select)."
  [^javax.swing.JComponent w] (.getClientProperty w id-property))

(def ^{:private true} h-alignment-table {
  :left     SwingConstants/LEFT 
  :right    SwingConstants/RIGHT
  :leading  SwingConstants/LEADING
  :trailing SwingConstants/TRAILING
  :center   SwingConstants/CENTER 
})

(def ^{:private true} v-alignment-table {
  :top    SwingConstants/TOP 
  :center SwingConstants/CENTER 
  :bottom SwingConstants/BOTTOM 
})

(def ^{:private true} orientation-table {
  :horizontal SwingConstants/HORIZONTAL
  :vertical   SwingConstants/VERTICAL
})

(defn- id-option-handler [w id]
  (let [id-key (name id)]
    (.putClientProperty w id-property id-key)
    (swap! widget-by-id assoc id-key [w])))

(def ^{:private true} default-options {
  :id          id-option-handler
  :listen      #(apply sse/add-listener %1 %2)
  :opaque      #(.setOpaque %1 %2)
  :enabled     #(.setEnabled %1 %2)
  :background  #(.setBackground %1 (to-color %2))
  :foreground  #(.setForeground %1 (to-color %2))
  :border      #(.setBorder %1 (to-border %2))
  :font        #(.setFont %1 (to-font %2))
  :tip         #(.setToolTipText %1 (str %2))
  :text        #(.setText %1 (str %2))
  :icon        #(.setIcon %1 (make-icon %2))
  :action      #(.setAction %1 %2)
  :selected    #(.setSelected %1 %2)
  :editable    #(.setEditable %1 %2)
  :halign      #(.setHorizontalAlignment %1 (h-alignment-table %2))
  :valign      #(.setVerticalAlignment %1 (v-alignment-table %2)) 
  :orientation #(.setOrientation %1 (orientation-table %2))
  :items       #(add-widgets %1 %2)
})

(defn apply-options
  [target opts handler-map]
  (doseq [[k v] (if (map? opts) opts (partition 2 opts))]
    (when-let [f (get handler-map k)]
      (f target v)))
  target)

(defn apply-default-opts
  ([p] (apply-default-opts p {}))
  ([^javax.swing.JComponent p {:as opts}]
    (apply-options p opts default-options)))


;*******************************************************************************
; Border Layout

(def ^{:private true}  border-layout-dirs {
  :north  BorderLayout/NORTH
  :south  BorderLayout/SOUTH
  :east   BorderLayout/EAST
  :west   BorderLayout/WEST
  :center BorderLayout/CENTER
})

(defn- border-layout-add [p w dir]
  (add-widget p w (dir border-layout-dirs)))

(def ^{:private true} border-layout-options 
  (apply hash-map
    (flatten 
      (for [dir (keys border-layout-dirs)]
        [dir  #(border-layout-add %1 %2 dir)]))))

(defn border-panel
  [& {:keys [hgap vgap] :or {hgap 0 vgap 0} :as opts}]
  (let [^java.awt.Container p (JPanel. (BorderLayout. hgap vgap))]
    (apply-options p opts (merge default-options border-layout-options))))

;*******************************************************************************
; Flow

(def ^{:private true} flow-align-table {
  :left     FlowLayout/LEFT 
  :right    FlowLayout/RIGHT
  :leading  FlowLayout/LEADING
  :trailing FlowLayout/TRAILING
  :center   FlowLayout/CENTER 
})

(defn flow-panel
  [& {:keys [hgap vgap align align-on-baseline] 
      :or {hgap 5 vgap 5 align :center align-on-baseline false} 
      :as opts}]
  (let [l (FlowLayout. (align flow-align-table) hgap vgap)]
    (.setAlignOnBaseline l align-on-baseline)
    (apply-options (JPanel. l) opts default-options)))

;*******************************************************************************
; Boxes

(def ^{:private true} box-layout-dir-table {
  :horizontal BoxLayout/X_AXIS 
  :vertical BoxLayout/Y_AXIS 
})

(defn box-panel
  [dir & opts]
  (let [panel  (JPanel.)
        layout (BoxLayout. panel (dir box-layout-dir-table))]
    (.setLayout panel layout)
    (apply-options panel opts default-options)))

(defn horizontal-panel [& opts] (apply box-panel :horizontal opts))
(defn vertical-panel [& opts] (apply box-panel :vertical opts))

;*******************************************************************************
; Grid

(defn grid-panel
  [& {:keys [hgap vgap rows columns] 
      :or {hgap 0 vgap 0}
      :as opts}]
  (let [columns* (or columns (if rows 0 1))
        layout   (GridLayout. (or rows 0) columns* hgap vgap)
        panel    (JPanel. layout)]
    (apply-options panel opts default-options)))

;*******************************************************************************
; Labels

(defn label 
  [& args]
  (if (next args)
    (apply-options (JLabel.) args default-options)
    (apply label :text args)))


;*******************************************************************************
; Buttons

(defn- apply-button-defaults
  [button args]
  (apply-options button args default-options))

(defn button   [& args] (apply-button-defaults (JButton.) args))
(defn toggle   [& args] (apply-button-defaults (JToggleButton.) args))
(defn checkbox [& args] (apply-button-defaults (JCheckBox.) args))
(defn radio    [& args] (apply-button-defaults (JRadioButton.) args))

;*******************************************************************************
; Text widgets

(defn text
  "Create a text field or area. Given a single argument, creates a JTextField using the argument as the initial text value. Otherwise, supports the following properties:

    :text         Initial text content
    :multi-line?  If true, a JTextArea is created (default false)
    :editable     If false, the text is read-only (default true)

  To listen for document changes, use the :listen option:

    (text :listen [:document #(... handler ...)])

  or attach a listener later with (listen):
    
    (text :id :my-text ...)
        ...
    (listen (select :#my-text) :documen #(... handler ...))
  " 
  [& args]
  (if (next args)
    (let [{:keys [multi-line?] :as opts} args
          t (if multi-line? (JTextArea.) (JTextField.))]
      (apply-options t opts default-options))
    (apply text :text args)))


;*******************************************************************************
; Scrolling

(defn scrollable 
  [w]
  (let [sp (JScrollPane. w)]
    sp))

;*******************************************************************************
; Splitter
(defn splitter
  [dir left right & opts]
  (JSplitPane. (dir {:left-right JSplitPane/HORIZONTAL_SPLIT
                     :top-bottom JSplitPane/VERTICAL_SPLIT})
               (to-widget left)
               (to-widget right)))

(defn left-right-split [& args] (apply splitter :left-right args))
(defn top-bottom-split [& args] (apply splitter :top-bottom args))


;*******************************************************************************
; Toolbars


(defn- insert-toolbar-separators 
  "Replace :separator with JToolBar$Separator instances"
  [items]
  (map #(if (= % :separator) (javax.swing.JToolBar$Separator.) %) items))

(def ^{:private true} toolbar-options {
  :floatable #(.setFloatable %1 %2)
  ; Override default :items handler
  :items     #(add-widgets %1 (insert-toolbar-separators %2))
})

(defn toolbar
  [& opts]
  (apply-options (JToolBar.) opts (merge default-options toolbar-options)))

;*******************************************************************************
; Tabs

(def ^{:private true} tab-placement-table {
  :bottom JTabbedPane/BOTTOM
  :top    JTabbedPane/TOP
  :left   JTabbedPane/LEFT
  :right  JTabbedPane/RIGHT 
})

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
        true (.addTab (when-not title-cmp (str title)) (make-icon icon) (to-widget content) (str tip))
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
(defn frame
  "Create a JFrame. Options:

    :title    the title of the window
    :pack?     true/false whether JFrame/pack should be called (default true)
    :width    initial width if :pack is false
    :height   initial height if :pack is true
    :content  passed through (to-widget) and used as the frame's content-pane
    :visible?  whether frame should be initially visible (default true)

  returns the new frame."

  [& {:keys [title width height content visible? pack?] 
      :or {width 100 height 100 visible? true pack? true}
      :as opts}]
  (cond-doto (JFrame.)
    title    (.setTitle (str title))
    content  (.setContentPane (to-widget content))
    true     (.setSize width height)
    true     (.setVisible visible?)
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
  ([v]
    (if-let [[_ id] (re-find id-regex (name v))]
      (get @widget-by-id id))))

