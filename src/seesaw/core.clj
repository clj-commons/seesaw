(ns seesaw.core
  (:use seesaw.font)
  (:use seesaw.border)
  (:use seesaw.color)
  (:import (java.net URL MalformedURLException)
           (javax.swing 
             SwingUtilities SwingConstants 
             Icon Action AbstractAction ImageIcon
             BoxLayout BorderFactory
             JFrame JComponent Box JPanel JScrollPane JSplitPane
             JLabel JTextField JTextArea 
             JButton JToggleButton JCheckBox JRadioButton)
           (javax.swing.event ChangeListener DocumentListener)
           (javax.swing.border Border)
           (java.awt Component FlowLayout BorderLayout GridLayout Dimension ItemSelectable)
           (java.awt.event MouseAdapter ActionListener)))

(defn- to-url [s]
  (try
    (URL. (str s))
   (catch MalformedURLException e
    nil)))

(defn invoke-later [f] (SwingUtilities/invokeLater f))
(defn invoke-now [f] (SwingUtilities/invokeAndWait f))

;*******************************************************************************
; Icons

(defn icon [p]
  (cond
    (nil? p) nil 
    (instance? javax.swing.Icon p) p
    (instance? java.net.URL p) (ImageIcon. p)
    true  (ImageIcon. (to-url p))))

(def ^{:private true} make-icon icon)

;*******************************************************************************
; Actions

(defn action [f & {:keys [name tip icon] :or { name "" }}]
  (doto (proxy [AbstractAction] [] (actionPerformed [e] (f e)))
    (.putValue Action/NAME (str name))
    (.putValue Action/SHORT_DESCRIPTION tip)
    (.putValue Action/SMALL_ICON (make-icon icon))))

;*******************************************************************************
; Generic widget stuff

(defn apply-mouse-handlers
  [p opts]
  (when (some #{:on-mouse-clicked :on-mouse-entered :on-mouse-exited} (keys opts))
    (.addMouseListener p
      (proxy [MouseAdapter] []
        (mouseClicked [e] (if-let [f (:on-mouse-clicked opts)] (f e)))
        (mouseEntered [e] (if-let [f (:on-mouse-entered opts)] (f e)))
        (mouseExited [e] (if-let [f (:on-mouse-exited opts)] (f e))))))
  p)

(defn apply-action-handler
  [p opts]
  (if-let [f (:on-action opts)]
    (if (instance? javax.swing.Action f)
      (.addActionListener p f)
      (.addActionListener p (action f))))
  p)

(defn apply-state-changed-handler
  [p opts]
  (if-let [f (:on-state-changed opts)]
    (if (instance? javax.swing.event.ChangeListener f)
      (.addChangeListener p f)
      (.addChangeListener p
        (reify javax.swing.event.ChangeListener
          (stateChanged [this e] (f e))))))
  p)

(defn apply-selection-changed-handler
  [p opts]
  (if-let [f (:on-selection-changed opts)]
    (if (instance? java.awt.ItemSelectable p)
      (if (instance? java.awt.event.ItemListener f)
        (.addItemListener p f)
        (.addItemListener p
          (reify java.awt.event.ItemListener
            (itemStateChanged [this e] (f e)))))))
  p)


(defn apply-default-opts
  ([p] (apply-default-opts p {}))
  ([p {:keys [opaque background foreground border font] :as opts}]
    (when (or (true? opaque) (false? opaque)) (.setOpaque p opaque))
    (when background (.setBackground p (to-color background)))
    (when foreground (.setForeground p (to-color foreground)))
    (when border (.setBorder p (to-border border)))
    (when font (.setFont p (to-font font)))
    (-> p
      (apply-mouse-handlers opts)
      (apply-action-handler opts)
      (apply-state-changed-handler opts)
      (apply-selection-changed-handler opts))))

(defn to-widget [v]
  (let [vs (when (coll? v) (seq v))]
    (cond
      (nil? v) nil
      (instance? java.awt.Dimension v) (Box/createRigidArea v)
      (instance? java.awt.Component v) v
      (instance? javax.swing.Action v) (JButton. v)
      (= v :fill-h) (Box/createHorizontalGlue)
      (= v :fill-v) (Box/createVerticalGlue)
      (= :fill-h (first vs)) (Box/createHorizontalStrut (second vs))
      (= :fill-v (first vs)) (Box/createVerticalStrut (second vs))
      (= :by (second vs)) (Box/createRigidArea (Dimension. (nth vs 0) (nth vs 2)))
      true (if-let [u (to-url v)] (JLabel. (make-icon u)) (JLabel. (str v))))))

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


;*******************************************************************************
; Border Layout

(def ^{:private true}  border-dirs {
  :north BorderLayout/NORTH
  :south BorderLayout/SOUTH
  :east BorderLayout/EAST
  :west BorderLayout/WEST
  :center BorderLayout/CENTER})

(defn- border-layout-add [p w dir]
  (when w (add-widget p w (dir border-dirs)))
  p)

(defn border-panel
  [& {:keys [north south east west center hgap vgap] :or {hgap 0 vgap 0} :as opts}]
  (let [p (apply-default-opts (JPanel.) opts)]
    (.setLayout p (BorderLayout. hgap vgap))
    (-> p
      (border-layout-add north :north)
      (border-layout-add south :south)
      (border-layout-add east :east)
      (border-layout-add west :west)
      (border-layout-add center :center))))

;*******************************************************************************
; Flow

(def ^{:private true} flow-align-table
  { :left FlowLayout/LEFT 
    :right FlowLayout/RIGHT
    :leading FlowLayout/LEADING
    :trailing FlowLayout/TRAILING
    :center FlowLayout/CENTER })

(defn flow-panel
  [& {:keys [hgap vgap align items align-on-baseline] 
      :or {hgap 5 vgap 5 align :center items [] align-on-baseline false} 
      :as opts}]
  (let [p (apply-default-opts (JPanel.) opts)
        l (FlowLayout. (align flow-align-table) hgap vgap)]
    (.setAlignOnBaseline l align-on-baseline)
    (.setLayout p l)
    (add-widgets p items)))

;*******************************************************************************
; Boxes

(defn box-panel
  [dir & {:keys [items] :as opts }]
  (let [p (apply-default-opts (JPanel.) opts)
        b (BoxLayout. p (dir { :horizontal BoxLayout/X_AXIS :vertical BoxLayout/Y_AXIS }))]
    (.setLayout p b)
    (add-widgets p items)))

(defn horizontal-panel [& opts] (apply box-panel :horizontal opts))
(defn vertical-panel [& opts] (apply box-panel :vertical opts))

;*******************************************************************************
; Grid

(defn grid-panel
  [& {:keys [hgap vgap rows columns items] 
      :or {hgap 0 vgap 0 items []}
      :as opts}]
  (let [p (apply-default-opts (JPanel.) opts)
        columns* (or columns (if rows 0 1))
        layout (GridLayout. (or rows 0) columns* hgap vgap)]
    (.setLayout p layout)
    (add-widgets p items)))

;*******************************************************************************
; Labels

(def ^{:private true} h-alignment-table
  { :left SwingConstants/LEFT 
    :right SwingConstants/RIGHT
    :leading SwingConstants/LEADING
    :trailing SwingConstants/TRAILING
    :center SwingConstants/CENTER })

(def ^{:private true} v-alignment-table
  { :top SwingConstants/TOP 
    :center SwingConstants/CENTER 
    :bottom SwingConstants/BOTTOM 
   })

(defn- apply-text-alignment 
  [w {:keys [halign valign]}]
  (let [hc (h-alignment-table halign)
        vc (v-alignment-table valign)]
    (when hc (.setHorizontalAlignment w hc))
    (when vc (.setVerticalAlignment w vc))
    w))

(defn label 
  [& {:keys [text icon] :as opts}]
  (let [w (-> (JLabel.) (apply-default-opts opts) (apply-text-alignment opts))]
    (when text (.setText w (str text)))
    (when icon (.setIcon w (make-icon icon)))
    w))


;*******************************************************************************
; Buttons

(defn- toggle-button
  [w & {:keys [text icon selected] :or {selected false} :as opts}]
  (let [w* (-> w (apply-default-opts opts) (apply-text-alignment opts))]
    (when text (.setText w* (str text)))
    (when icon (.setIcon w* (make-icon icon)))
    (.setSelected w* selected)
    w*))

(defn toggle [& args] (apply toggle-button (JToggleButton.) args))
(defn checkbox [& args] (apply toggle-button (JCheckBox.) args))
(defn radio [& args] (apply toggle-button (JRadioButton.) args))

;*******************************************************************************
; Text widgets

(defn add-document-listener [w f]
  (when f
    (.. w 
      getDocument
      (addDocumentListener
        (if (instance? DocumentListener f)
          f
          (proxy [DocumentListener] []
            (insertUpdate [e] (f e))
            (removeUpdate [e] (f e))
            (changedUpdate []))))))
  w)

(defn apply-text-opts 
  [w { :keys [on-changed text editable] :or { editable true } :as opts }]
  (let [w* (apply-default-opts w opts)]
    (.setEditable w editable)
    (when text (.setText w (str text)))
    (add-document-listener w on-changed)
    w*))
  
(defn text
  [& {:keys [text multi-line?] :as opts}]
  (let [t (if multi-line? (JTextArea.) (apply-text-alignment (JTextField.) opts))
        w (apply-text-opts t opts)]
    (when text (.setText w (str text)))
    w))


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
; Frame
(defn frame
  [& {:keys [title width height content visible pack] 
      :or {width 100 height 100 visible true pack true}
      :as opts}]
  (let [f (JFrame.)]
    (when title (.setTitle f title))
    (when content (.setContentPane f content))
    (doto f
      (.setSize width height)
      (.setVisible visible))
    (when pack (.pack f))
    f))


