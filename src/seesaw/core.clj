(ns seesaw.core
  (:import (java.net URL MalformedURLException)
           (javax.swing Icon Action AbstractAction ImageIcon
            BoxLayout Box BorderFactory
            JComponent JLabel JButton JFrame JPanel JScrollPane
            JTextField JTextArea)
           (javax.swing.border Border)
           (java.awt Color Component BorderLayout Dimension)
           (java.awt.event MouseAdapter)))

(defn to-url [s]
  (try
    (URL. (str s))
   (catch MalformedURLException e
    nil)))

;*******************************************************************************
; Icons

(defn make-icon [p]
  (let [c (class p)]
    (cond
      (nil? p) nil 
      (isa? c javax.swing.Icon) p
      (isa? c java.net.URL) (ImageIcon. p)
      true  (ImageIcon. (to-url p)))))

;*******************************************************************************
; Actions

(defn make-action [f & {:keys [name tip icon] :or { name "Act" }}]
  (doto (proxy [AbstractAction] [] (actionPerformed [e] (f e)))
    (.putValue Action/NAME name)
    (.putValue Action/SHORT_DESCRIPTION tip)
    (.putValue Action/SMALL_ICON (make-icon icon))))

;*******************************************************************************
; Borders

(declare to-border)

(defn empty-border 
  [& {:keys [thickness top left bottom right]}]
  (if (or top left bottom right)
    (BorderFactory/createEmptyBorder (or top 0) (or left 0) (or bottom 0) (or right 0))
    (let [t (or thickness 1)]
      (BorderFactory/createEmptyBorder t t t t))))

(defn line-border 
  [& {:keys [color thickness top left bottom right]}]
  (if (or top left bottom right)
    (BorderFactory/createMatteBorder (or top 0) (or left 0) (or bottom 0) (or right 0) (or color Color/BLACK))
    (BorderFactory/createLineBorder (or color Color/BLACK) (or thickness 1))))

(defn compound-border
  ([b] (to-border b))
  ([b0 b1] (BorderFactory/createCompoundBorder (to-border b1) (to-border b0)))
  ([b0 b1 & more] (reduce #(compound-border %1 %2) (compound-border b0 b1) more)))

(defn to-border 
  ([b] 
    (if (isa? (class b) javax.swing.border.Border) 
      b
      (if (coll? b)
        (apply to-border b)
        (BorderFactory/createTitledBorder (str b)))))
  ([b & args]
    (apply compound-border b args)))

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

(defn apply-default-opts
  ([p] (apply-default-opts p {}))
  ([p {:keys [opaque background foreground border] :as opts}]
    (apply-mouse-handlers p opts)
    (when (or (true? opaque) (false? opaque)) (.setOpaque p opaque))
    (when background (.setBackground p background))
    (when foreground (.setForeground p foreground))
    (when border (.setBorder p (to-border border)))
    p))

(defn to-widget [v]
  (let [c (class v)
        vs (when (coll? v) (seq v))]
    (cond
      (= v :fill-h) (Box/createHorizontalGlue)
      (= v :fill-v) (Box/createVerticalGlue)
      (= :fill-h (first vs)) (Box/createHorizontalStrut (second vs))
      (= :fill-v (first vs)) (Box/createVerticalStrut (second vs))
      (isa? c java.awt.Dimension) (Box/createRigidArea v)
      (= :by (second vs)) (Box/createRigidArea (Dimension. (nth vs 0) (nth vs 2)))
      (isa? c java.awt.Component) v
      (isa? c javax.swing.Action) (JButton. v)
      true (if-let [u (to-url v)] (JLabel. (make-icon u)) (JLabel. (str v))))))

(defn- add-widget 
  ([c w] (.add c (to-widget w)))
  ([c w constraint] (.add c (to-widget w) constraint)))

;*******************************************************************************
; Border Layout

(def border-dirs {
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
    (border-layout-add p north :north)
    (border-layout-add p south :south)
    (border-layout-add p east :east)
    (border-layout-add p west :west)
    (border-layout-add p center :center)
    p))

;*******************************************************************************
; Boxes

(defn box-panel
  [dir & {:keys [items] :as opts }]
  (let [p (apply-default-opts (JPanel.) opts)
        b (BoxLayout. p (dir { :horizontal BoxLayout/X_AXIS :vertical BoxLayout/Y_AXIS }))]
    (.setLayout p b)
    (doseq [w items]
      (.add p (to-widget w)))
    p))

(defn horizontal-panel [& opts] (apply box-panel :horizontal opts))
(defn vertical-panel [& opts] (apply box-panel :vertical opts))

;*******************************************************************************
; Labels

(defn label 
  [& {:keys [text icon] :as opts}]
  (let [w (apply-default-opts (JLabel.) opts)]
    (when text (.setText w (str text)))
    (when icon (.setIcon w (make-icon icon)))
    w))

;*******************************************************************************
; Text widgets

(defn apply-text-opts 
  [w { :keys [on-change text editable] :or { editable true } :as opts }]
  (let [w* (apply-default-opts w opts)]
    (.setEditable w editable)
    (when text (.setText w (str text)))
    w*))
  
(defn text
  [& {:keys [text multi-line?] :as opts}]
  (let [t (if multi-line? (JTextArea.) (JTextField.))
        w (apply-text-opts t opts)]
    (when text (.setText w (str text)))
    w))


;*******************************************************************************
; Scrolling

(defn scrollable 
  [w]
  (let [sp (JScrollPane. w)]
    sp))



