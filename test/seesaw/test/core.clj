(ns seesaw.test.core
  (:use seesaw.core
        seesaw.font)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [javax.swing SwingConstants
                        Action
                        JFrame
                        JToolBar
                        JPanel JLabel JButton JTextField JTextArea Box Box$Filler BoxLayout
                        JToggleButton JCheckBox JRadioButton
                        JScrollPane
                        JSplitPane]
           [java.awt Insets Color Dimension FlowLayout]
           [java.awt.event ActionEvent]))

(describe action
  (it "sets the name and tooltip"
    (let [a (action (fn [e]) :name "Test" :tip "This is a tip")]
      (expect (= "Test" (.getValue a Action/NAME)))
      (expect (= "This is a tip" (.getValue a Action/SHORT_DESCRIPTION))))))


(describe apply-default-opts 
  (testing "setting opaque option"
    (it "does nothing when omitted"
      (let [c (apply-default-opts (JPanel.))]
        (expect (= true (.isOpaque c)))))
    (it "sets opacity when provided"
      (let [c (apply-default-opts (JPanel.) {:opaque false})]
        (expect (= false (.isOpaque c))))))
  (it "sets background using to-color when provided"
      (let [c (apply-default-opts (JPanel.) {:background "#000000" })]
        (expect (= Color/BLACK (.getBackground c)))))
  (it "sets foreground when provided"
      (let [c (apply-default-opts (JPanel.) {:foreground "#00FF00" })]
        (expect (= Color/GREEN (.getForeground c)))))
  (it "sets border when provided using to-border"
      (let [c (apply-default-opts (JPanel.) {:border "TEST"})]
        (expect (= "TEST" (.. c getBorder getTitle))))))

(describe to-widget
  (it "returns nil if input is nil"
      (= nil (to-widget nil)))
  (it "returns input if it's already a widget"
    (let [c (JPanel.)]
      (expect (= c (to-widget c)))))
  (it "does not create a new widget if create? param is false"
    (expect (nil? (to-widget "HI" false))))
  (it "returns a label for text input"
    (let [c (to-widget "TEST")]
      (expect (= "TEST" (.getText c)))))
  (it "returns a button if input is an Action"
    (let [a (action #(println "HI") :name "Test")
          c (to-widget a)]
      (expect (isa? (class c) javax.swing.JButton))
      (expect (= "Test" (.getText c)))))
  (it "creates horizontal glue for :fill-h"
    (let [c (to-widget :fill-h)]
      (expect (isa? (class c) javax.swing.Box$Filler ))
      (expect (= 32767 (.. c getMaximumSize getWidth)))))
  (it "creates vertical glue for :fill-v"
    (let [c (to-widget :fill-v)]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 32767 (.. c getMaximumSize getHeight)))))
  (it "creates a vertical strut for [:fill-v N]"
    (let [c (to-widget [:fill-v 99])]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 32767 (.. c getMaximumSize getWidth)))
      (expect (= 99 (.. c getMaximumSize getHeight)))
      (expect (= 99 (.. c getPreferredSize getHeight)))))
  (it "creates a horizontal strut for [:fill-h N]"
    (let [c (to-widget [:fill-h 88])]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 32767 (.. c getMaximumSize getHeight)))
      (expect (= 88 (.. c getMaximumSize getWidth)))
      (expect (= 88 (.. c getPreferredSize getWidth)))))
  (it "creates a rigid area for a Dimension"
    (let [c (to-widget (Dimension. 12 34))]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 12 (.. c getMaximumSize getWidth)))
      (expect (= 34 (.. c getMaximumSize getHeight)))
      (expect (= 12 (.. c getPreferredSize getWidth)))
      (expect (= 34 (.. c getPreferredSize getHeight)))))
  (it "creates a rigid area for a [N :by N]"
    (let [c (to-widget [12 :by 34])]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 12 (.. c getMaximumSize getWidth)))
      (expect (= 34 (.. c getMaximumSize getHeight)))
      (expect (= 12 (.. c getPreferredSize getWidth)))
      (expect (= 34 (.. c getPreferredSize getHeight)))))
  (it "converts an event to its source"
    (let [b (button)
          e (ActionEvent. b 0 "hi")]
      (expect (= b (to-widget e))))))

(describe flow-panel
  (it "should create a FlowLayout of :items list"
    (let [[a b c] [(JPanel.) (JPanel.) (JPanel.)]
          p (flow-panel :items [a b c] :align :trailing :hgap 99 :vgap 12 :align-on-baseline true)
          l (.getLayout p)]
      (expect (= java.awt.FlowLayout (class l)))
      (expect (= FlowLayout/TRAILING (.getAlignment l)))
      (expect (= 99 (.getHgap l)))
      (expect (= 12 (.getVgap l)))
      (expect (= true (.getAlignOnBaseline l)))
      (expect (= [a b c] (seq (.getComponents p)))))))

(describe horizontal-panel
  (it "should create a horizontal box of :items list"
    (let [[a b c] [(JPanel.) (JPanel.) (JPanel.)]
          p (horizontal-panel :items [a b c])]
      (expect (= BoxLayout/X_AXIS (.. p getLayout getAxis)))
      (expect (= [a b c] (seq (.getComponents p)))))))

(describe vertical-panel
  (it "should create a vertical box of :items list"
    (let [[a b c] [(JPanel.) (JPanel.) (JPanel.)]
          p (vertical-panel :items [a b c])]
      (expect (= BoxLayout/Y_AXIS (.. p getLayout getAxis)))
      (expect (= [a b c] (seq (.getComponents p)))))))

(describe grid-panel
  (it "should default to 1 column"
    (let [g (grid-panel)
          l (.getLayout g)]
      (expect (= 0 (.getRows l)))
      (expect (= 1 (.getColumns l)))))
  (it "should set number of rows"
    (let [g (grid-panel :rows 12)
          l (.getLayout g)]
      (expect (= 12 (.getRows l)))
      (expect (= 0 (.getColumns l)))))
  (it "should set the hgap and vgap"
    (let [g (grid-panel :hgap 2 :vgap 3)
          l (.getLayout g)]
      (expect (= 2 (.getHgap l)))
      (expect (= 3 (.getVgap l)))))
  (it "should add the given items to the panel"
    (let [[a b c] [(label :text "A") (label :text "B") (label :text "C")]
          g (grid-panel :items [a b c])] 
      (expect (= [a b c] (seq (.getComponents g)))))))

(describe "for an arbitrary widget"
  (it "should support the :font property"
    (let [f (font "ARIAL-BOLD-18")
          l (label :font f)]
      (expect (= f (.getFont l))))))

(describe label
  (it "should create a label"
    (expect (= JLabel (class (label)))))
  (it "should create a label with tooltip"
    (expect (= "HI" (.getToolTipText (label :tip "HI")))))
  (it "should create a label with text when given a single argument"
    (expect (= "test label" (.getText (label "test label")))))
  (it "should create a label with text"
    (expect (= "test label" (.getText (label :text "test label")))))
  (it "should create a label with horizontal alignment"
    (= SwingConstants/LEFT (.getHorizontalAlignment (label :halign :left))))
  (it "should create a label with vertical alignment"
    (= SwingConstants/BOTTOM (.getVerticalAlignment (label :valign :bottom)))))

(describe text
  (it "should create a text field given a string argument"
    (let [t (text "HI")]
      (expect (= JTextField (class t)))
      (expect (= "HI" (.getText t)))))
  (it "should create a text field by default"
    (let [t (text :text "HI")]
      (expect (= JTextField (class t)))
      (expect (= "HI" (.getText t)))))
  (it "should create a text area when multi-line? is true"
    (let [t (text :text "HI" :multi-line? true)]
      (expect (= JTextArea (class t)))
      (expect (= "HI" (.getText t)))))
  (it "should honor the editable property"
    (let [t (text :text "HI" :editable false :multi-line? true)]
      (expect (false? (.isEditable t))))))

(describe button
  (it "should create a JButton"
    (let [b (button :text "HI")]
      (expect (= JButton (class b)))
      (expect (= "HI" (.getText b)))))
  (it "should create a button from an action"
    (let [a (action println)
          b (button :action a)]
      (expect (= JButton (class b)))
      (expect (= a (.getAction b))))))

(describe toggle
  (it "should create a JToggleButton"
    (let [t (toggle :text "HI")]
      (expect (= JToggleButton (class t)))
      (expect (= "HI" (.getText t))))
      (expect (not (.isSelected t))))
  (it "should honor the :selected property"
    (let [t (toggle :text "HI" :selected true)]
      (expect (.isSelected t)))))

(describe checkbox
  (it "should create a JCheckBox"
    (let [t (checkbox :text "HI")]
      (expect (= JCheckBox (class t)))
      (expect (= "HI" (.getText t))))
      (expect (not (.isSelected t))))
  (it "should honor the :selected property"
    (let [t (checkbox :text "HI" :selected true)]
      (expect (.isSelected t)))))

(describe radio
  (it "should create a JRadioButton"
    (let [t (radio :text "HI")]
      (expect (= JRadioButton (class t)))
      (expect (= "HI" (.getText t))))
      (expect (not (.isSelected t))))
  (it "should honor the :selected property"
    (let [t (radio :text "HI" :selected true)]
      (expect (.isSelected t)))))

(describe scrollable
  (it "should create a JScrollPane"
    (let [l (label :text "Test")
          s (scrollable l)]
      (expect (= JScrollPane (class s)))
      (expect (= l (.. s getViewport getView))))))

(describe splitter
  (it "should create a JSplitPane with with two panes"
    (let [left (label :text "Left")
          right (label :text "Right")
          s (splitter :left-right left right)]
      (expect (= javax.swing.JSplitPane (class s)))
      (expect (= left (.getLeftComponent s)))
      (expect (= right (.getRightComponent s))))))

(describe toolbar
  (it "should create a JToolBar with the given items"
    (let [tb (toolbar :items ["a" "b" "c"])
          items (.getComponents tb)]
      (expect (= javax.swing.JToolBar (class tb)))
      (expect (= ["a" "b" "c"] (map #(.getText %) items)))))
  (it "should set the floatable property"
    (let [tb (toolbar :floatable true)]
      (expect (.isFloatable tb))))
  (it "should set the floatable property to false"
    (let [tb (toolbar :floatable false)]
      (expect (not (.isFloatable tb)))))
  (it "should set the orientation property"
    (let [tb (toolbar :orientation :vertical)]
      (expect (= SwingConstants/VERTICAL (.getOrientation tb)))))
  (it "can create a separator with the :separator keyword"
    (let [tb (toolbar :items [:separator])]
      (expect (= javax.swing.JToolBar$Separator (class (.getComponent tb 0)))))))

(describe frame
  (it "should create a JFrame and set its title, width, and height"
    (let [f (frame :title "Hello" :width 99 :height 88 :visible? false)]
      (expect (= javax.swing.JFrame (class f)))
      (expect (= "Hello" (.getTitle f)))))
  (it "should create a JFrame and set its content pane"
    (let [c (label :text "HI")
          f (frame :content c :visible? false)]
      (expect (= c (.getContentPane f))))))

(describe to-frame
  (it "should convert a widget to its parent frame"
    (let [c (label :text "HI")
          f (frame :content c :visible? false)]
      (expect (= f (to-frame c)))))
  (it "should return nil for an un-parented widget"
    (let [c (label :text "HI")]
      (expect (nil? (to-frame c))))))

