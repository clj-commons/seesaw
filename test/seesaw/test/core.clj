;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.core
  (:use seesaw.core
        seesaw.font)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [javax.swing SwingConstants
                        Action
                        JFrame
                        JToolBar JTabbedPane
                        JPanel JLabel JButton JTextField JTextArea Box Box$Filler BoxLayout
                        JToggleButton JCheckBox JRadioButton
                        JScrollPane
                        JSplitPane]
           [java.awt Insets Color Dimension FlowLayout BorderLayout]
           [java.awt.event ActionEvent]))

(describe apply-options
  (it "throws IllegalArgumentException if properties aren't event"
    (try
      (do (apply-options (JPanel.) [1 2 3] {}) false)
      (catch IllegalArgumentException e true)))
  (it "throws IllegalArgumentException for an unknown property"
    (try
      (do (apply-options (JPanel.) [:unknown "unknown"] {}) false)
      (catch IllegalArgumentException e true))))

(describe action
  (it "sets the name and tooltip"
    (let [a (action (fn [e]) :name "Test" :tip "This is a tip")]
      (expect (= "Test" (.getValue a Action/NAME)))
      (expect (= "This is a tip" (.getValue a Action/SHORT_DESCRIPTION))))))

(describe id-for
  (it "returns nil if a widget doesn't have an id"
    (nil? (id-for (label))))
  (it "returns the correct id if a widget has an id"
    (= "id of the label" (id-for (label :id "id of the label")))))

(describe "Applying default options"
  (testing "the :id option"
    (it "does nothing when omitted"
      (expect (nil? (-> (JPanel.) apply-default-opts id-for))))
    (it "sets the component's name if given"
      (expect "hi" (-> (JLabel.) (apply-default-opts {:id "hi"}) id-for)))
    (it "throws IllegalStateException if the widget's id is already set"
      (try 
        (do (config (label :id :foo) :id :bar) false)
        (catch IllegalStateException e true))))
  (testing "setting enabled option"
    (it "does nothing when omitted"
      (let [c (apply-default-opts (JPanel.))]
        (expect (.isEnabled c))))
    (it "sets enabled when provided"
      (let [c (apply-default-opts (JPanel.) {:enabled? false})]
        (expect (not (.isEnabled c)))))
    (it "sets enabled when provided a truthy value"
      (let [c (apply-default-opts (JPanel.) {:enabled? "something"})]
        (expect (.isEnabled c))))
    (it "sets enabled when provided a falsey value"
      (let [c (apply-default-opts (JPanel.) {:enabled? nil})]
        (expect (= false (.isEnabled c))))))
  (testing "setting opaque? option"
    (it "does nothing when omitted"
      (let [c (apply-default-opts (JPanel.))]
        (expect (.isOpaque c))))
    (it "sets opacity when provided"
      (let [c (apply-default-opts (JPanel.) {:opaque? false})]
        (expect (not (.isOpaque c))))))
  (testing "the :model property"
    (it "sets the model when provided"
      (let [model  (javax.swing.DefaultButtonModel.)
            widget (apply-default-opts (button :model model))]
        (expect (= model (.getModel widget))))))
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
    (let [c (to-widget "TEST" true)]
      (expect (= "TEST" (.getText c)))))
  (it "returns a button if input is an Action"
    (let [a (action #(println "HI") :name "Test")
          c (to-widget a true)]
      (expect (isa? (class c) javax.swing.JButton))
      (expect (= "Test" (.getText c)))))
  (it "creates a separator for :separator"
    (expect (= javax.swing.JSeparator (class (to-widget :separator true)))))
  (it "creates horizontal glue for :fill-h"
    (let [c (to-widget :fill-h true)]
      (expect (isa? (class c) javax.swing.Box$Filler ))
      (expect (= 32767 (.. c getMaximumSize getWidth)))))
  (it "creates vertical glue for :fill-v"
    (let [c (to-widget :fill-v true)]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 32767 (.. c getMaximumSize getHeight)))))
  (it "creates a vertical strut for [:fill-v N]"
    (let [c (to-widget [:fill-v 99] true)]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 32767 (.. c getMaximumSize getWidth)))
      (expect (= 99 (.. c getMaximumSize getHeight)))
      (expect (= 99 (.. c getPreferredSize getHeight)))))
  (it "creates a horizontal strut for [:fill-h N]"
    (let [c (to-widget [:fill-h 88] true)]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 32767 (.. c getMaximumSize getHeight)))
      (expect (= 88 (.. c getMaximumSize getWidth)))
      (expect (= 88 (.. c getPreferredSize getWidth)))))
  (it "creates a rigid area for a Dimension"
    (let [c (to-widget (Dimension. 12 34) true)]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 12 (.. c getMaximumSize getWidth)))
      (expect (= 34 (.. c getMaximumSize getHeight)))
      (expect (= 12 (.. c getPreferredSize getWidth)))
      (expect (= 34 (.. c getPreferredSize getHeight)))))
  (it "creates a rigid area for a [N :by N]"
    (let [c (to-widget [12 :by 34] true)]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 12 (.. c getMaximumSize getWidth)))
      (expect (= 34 (.. c getMaximumSize getHeight)))
      (expect (= 12 (.. c getPreferredSize getWidth)))
      (expect (= 34 (.. c getPreferredSize getHeight)))))
  (it "converts an event to its source"
    (let [b (button)
          e (ActionEvent. b 0 "hi")]
      (expect (= b (to-widget e))))))

(describe config
  (it "configures the properties given to it on a single target"
    (let [p (JPanel.)
          result (config p :foreground Color/RED :background Color/BLUE :enabled? false)]
      (expect (= p result))
      (expect (= Color/RED (.getForeground p)))
      (expect (= Color/BLUE (.getBackground p)))
      (expect (not (.isEnabled p)))))
  (it "configures the properties given to it on a multiple targets"
    (let [targets [(JPanel.) (JPanel.)]
          result (config targets :foreground Color/RED :background Color/BLUE :enabled? false)]
      (expect (= targets result))
      (expect (= Color/RED (.getForeground (first targets))))
      (expect (= Color/BLUE (.getBackground (first targets))))
      (expect (not (.isEnabled (first targets))))
      (expect (= Color/RED (.getForeground (second targets))))
      (expect (= Color/BLUE (.getBackground (second targets))))
      (expect (not (.isEnabled (second targets))))))
  (it "configures a target with type-specific properties"
    (let [t (toggle :text "hi" :selected? false)]
      (expect (.isSelected (config t :selected? true))))))

(describe flow-panel
  (it "should create a FlowLayout of :items list"
    (let [[a b c] [(JPanel.) (JPanel.) (JPanel.)]
          p (flow-panel :items [a b c] :align :trailing :hgap 99 :vgap 12 :align-on-baseline true)
          l (.getLayout p)]
      (expect (= java.awt.FlowLayout (class l)))
      (expect (= FlowLayout/TRAILING (.getAlignment l)))
      (expect (= 99 (.getHgap l)))
      (expect (= 12 (.getVgap l)))
      (expect (.getAlignOnBaseline l))
      (expect (= [a b c] (seq (.getComponents p)))))))

(describe border-panel
  (it "should create a BorderLayout "
    (let [[n s e w c] [(JPanel.) (JPanel.) (JPanel.)(JPanel.)(JPanel.)]
          p (border-panel :hgap 99 :vgap 12 :north n :south s :east e :west w :center c)
          l (.getLayout p)]
      (expect (= java.awt.BorderLayout (class l)))
      (expect (= 99 (.getHgap l)))
      (expect (= 12 (.getVgap l)))
      (expect (= #{n s e w c} (apply hash-set (.getComponents p)))))))

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

(describe realize-grid-bag-constraints
  (it "should return a vector of widget/constraint pairs"
    (let [[[w0 c0] [w1 c1] & more] (realize-grid-bag-constraints [[:first :weightx 99 :weighty 555 :gridx :relative] [:second :weightx 100 :anchor :baseline]])]
      (expect (nil? more))
      (expect (= :first w0))
      (expect (= 99 (.weightx c0)))
      (expect (= 555 (.weighty c0)))
      (expect (= :second w1))
      (expect (= 100 (.weightx c1)))
      (expect (= 555 (.weighty c1))))))

(describe form-panel
  (it "should create a JPanel with a GridBagLayout"
    (= java.awt.GridBagLayout (class (.getLayout (form-panel)))))
  (it "should add an item with grid bag constraints"
    (let [p (form-panel :items [["hi" :weighty 999]])
          w (first (.getComponents p))
          gbcs (.getConstraints (.getLayout p) w)]
      (expect (instance? JLabel w))
      (expect (= java.awt.GridBagConstraints (class gbcs)))
      (expect (= 999 (.weighty gbcs))))))

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
  (it "should return the text of a single text widget argument"
    (= "HI" (text (text "HI"))))
  (it "should return the text of a button argument"
    (= "HI" (text (button :text "HI"))))
  (it "should return the text of a seq of widget arguments"
    (= ["HI" "BYE"] (text [(text "HI") (button :text "BYE")])))
  (it "should set the text of a single text widget argument"
    (= "BYE" (text (text (text "HI") "BYE"))))
  (it "should set the text of a single button argument"
    (= "BYE" (text (text (button :text "HI") "BYE"))))
  (it "should set the text of a seq of widget arguments"
    (let [[a b] [(text "HI") (text "BYE")]
          result (text [a b] "YUM")]
      (expect (= [a b] result))
      (expect (= "YUM" (text a)))
      (expect (= "YUM" (text b)))))
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
    (let [t (text :text "HI" :editable? false :multi-line? true)]
      (expect (not (.isEditable t))))))

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
    (let [t (toggle :text "HI" :selected? true)]
      (expect (.isSelected t)))))

(describe checkbox
  (it "should create a JCheckBox"
    (let [t (checkbox :text "HI")]
      (expect (= JCheckBox (class t)))
      (expect (= "HI" (.getText t))))
      (expect (not (.isSelected t))))
  (it "should honor the :selected property"
    (let [t (checkbox :text "HI" :selected? true)]
      (expect (.isSelected t)))))

(describe radio
  (it "should create a JRadioButton"
    (let [t (radio :text "HI")]
      (expect (= JRadioButton (class t)))
      (expect (= "HI" (.getText t))))
      (expect (not (.isSelected t))))
  (it "should honor the :selected property"
    (let [t (radio :text "HI" :selected? true)]
      (expect (.isSelected t)))))

(describe listbox
  (it "should create a JList"
    (let [lb (listbox)]
      (expect (= javax.swing.JList (class lb)))))
  (it "should create a JList using a seq as its model"
    (let [lb (listbox :model [1 2 3 4])
          model (.getModel lb)]
      (expect (= [1 2 3 4] (map #(.getElementAt model %1) (range (.getSize model))))))))

(describe combobox
  (it "should create a JComboBox"
    (let [lb (combobox)]
      (expect (= javax.swing.JComboBox (class lb)))))
  (testing "the :editable? property"
    (it "should create a non-editable JComboBox when false"
      (not (.isEditable (combobox :editable? false))))
    (it "should create an editable JComboBox when true"
      (.isEditable (combobox :editable? true))))
  (it "should create a JComboBox using a seq as its model"
    (let [lb (combobox :model [1 2 3 4])
          model (.getModel lb)]
      (expect (= [1 2 3 4] (map #(.getElementAt model %1) (range (.getSize model)))))
      (expect (= 1 (.getSelectedItem model))))))

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
  (it "should set the floatable? property"
    (let [tb (toolbar :floatable? true)]
      (expect (.isFloatable tb))))
  (it "should set the floatable property to false"
    (let [tb (toolbar :floatable? false)]
      (expect (not (.isFloatable tb)))))
  (it "should set the orientation property"
    (let [tb (toolbar :orientation :vertical)]
      (expect (= SwingConstants/VERTICAL (.getOrientation tb)))))
  (it "can create a toolbar separator with the :separator keyword"
    (let [tb (toolbar :items [:separator])]
      (expect (= javax.swing.JToolBar$Separator (class (.getComponent tb 0)))))))

(describe separator
  (it "should create a horizontal JSeparator by default"
    (let [s (separator)]
      (expect (= javax.swing.JSeparator (class s)))
      (expect (= SwingConstants/HORIZONTAL (.getOrientation s)))))
  (it "should create a horizontal JSeparator when :orientation is specified"
    (let [s (separator :orientation :horizontal)]
      (expect (= javax.swing.JSeparator (class s)))
      (expect (= SwingConstants/HORIZONTAL (.getOrientation s)))))
  (it "should create a vertical JSeparator when :orientation is specified"
    (let [s (separator :orientation :vertical)]
      (expect (= javax.swing.JSeparator (class s)))
      (expect (= SwingConstants/VERTICAL (.getOrientation s))))))

(describe mig-panel
  (it "should create a panel with a MigLayout"
    (expect (= net.miginfocom.swing.MigLayout (class (.getLayout (mig-panel))))))
  (it "should set MigLayout layout constraints"
    (let [p (mig-panel :constraints ["wrap 4", "[fill]", "[nogrid]"])
          l (.getLayout p)]
      (expect (= "wrap 4" (.getLayoutConstraints l)))
      (expect (= "[fill]" (.getColumnConstraints l)))
      (expect (= "[nogrid]" (.getRowConstraints l))))))

(describe tabbed-panel
  (it "should create a JTabbedPane with desired tab placement and layout"
    (let [tp (tabbed-panel :placement :bottom :overflow :wrap)]
      (expect (= JTabbedPane (class tp)))
      (expect (= JTabbedPane/BOTTOM (.getTabPlacement tp)))
      (expect (= JTabbedPane/WRAP_TAB_LAYOUT (.getTabLayoutPolicy tp)))))
  (it "should add tabs from the tabs property"
    (let [a (label "A tab")
          b (label "B tab")
          tp (tabbed-panel :tabs [{ :title "A" :content a :tip "tip A" } 
                                  { :title "B" :content b :tip "tip B" }])]
      (expect (= ["A" "B"]         [(.getTitleAt tp 0) (.getTitleAt tp 1)]))
      (expect (= ["tip A" "tip B"] [(.getToolTipTextAt tp 0) (.getToolTipTextAt tp 1)]))
      (expect (= [a b]             [(.getComponentAt tp 0) (.getComponentAt tp 1)])))))

(describe frame
  (it "should create a JFrame and set its title, width, and height"
    (let [f (frame :title "Hello" :width 99 :height 88 :visible? false)]
      (expect (= javax.swing.JFrame (class f)))
      (expect (= "Hello" (.getTitle f)))))
  (it "should create a JFrame and make is not resizable"
    (let [f (frame :title "Hello" :resizable? false :visible? false)]
      (expect (not (.isResizable f)))))
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

(describe select
  (it "should find a widget by #id and returns it"
    (let [c (label :id "hi")
          p (flow-panel :id :panel :items [c])
          f (frame :title "select by id" :visible? false :content p)]
      (expect (= c (select :#hi)))
      (expect (= p (select "#panel"))))))

