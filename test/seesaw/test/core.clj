;  Copyright (c) Dave Ray, 2011. All ritest/seesaw/test/core.clj

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.core
  (:use seesaw.core
        seesaw.font
        seesaw.graphics
        seesaw.cells)
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
        (do (config! (label :id :foo) :id :bar) false)
        (catch IllegalStateException e true))))
  (testing "the :preferred-size option"
    (it "set the component's preferred size using to-dimension"
      (let [p (apply-default-opts (JPanel.) {:preferred-size [10 :by 20]})]
        (expect (= (Dimension. 10 20) (.getPreferredSize p))))))
  (testing "the :minimum-size option"
    (it "set the component's minimum size using to-dimension"
      (let [p (apply-default-opts (JPanel.) {:minimum-size [10 :by 20]})]
        (expect (= (Dimension. 10 20) (.getMinimumSize p))))))
  (testing "the :maximum-size option"
    (it "set the component's maximum size using to-dimension"
      (let [p (apply-default-opts (JPanel.) {:maximum-size [10 :by 20]})]
        (expect (= (Dimension. 10 20) (.getMaximumSize p))))))
  (testing "the :size option"
    (it "set the component's min, max, and preferred size using to-dimension"
      (let [p (apply-default-opts (JPanel.) {:size [11 :by 21]})
            d (Dimension. 11 21)]
        (expect (= d (.getPreferredSize p)))
        (expect (= d (.getMinimumSize p)))
        (expect (= d (.getMaximumSize p))))))
  (testing "the :location option"
    (it "sets the component's location with a two-element vector"
      (let [p (apply-default-opts (JPanel.) {:location [23 45]})
            l (.getLocation p)]
        (expect (= [23 45] [(.x l) (.y l)]))))
    (it "sets the component's location with a java.awt.Point"
      (let [p (apply-default-opts (JPanel.) {:location (java.awt.Point. 23 45)})
            l (.getLocation p)]
        (expect (= [23 45] [(.x l) (.y l)])))))
  (testing "the :bounds option"
    (it "sets the component's bounds with a [x y width height] vector"
      (let [p (apply-default-opts (JPanel.) {:bounds [23 45 67 89]})
            b (.getBounds p)]
        (expect (= [23 45 67 89] [(.x b) (.y b) (.width b) (.height b)]))))
    (it "sets the component's bounds with a java.awt.Rectangle"
      (let [p (apply-default-opts (JPanel.) {:bounds (java.awt.Rectangle. 23 45 67 89)})
            b (.getBounds p)]
        (expect (= [23 45 67 89] [(.x b) (.y b) (.width b) (.height b)])))))

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
  (it "returns input if it's a JFrame"
    (let [c (JFrame.)]
      (expect (= c (to-widget c)))))
  (it "does not create a new widget if create? param is false"
    (expect (nil? (to-widget "HI" false))))
  (it "returns a label for text input"
    (let [c (to-widget "TEST" true)]
      (expect (= "TEST" (.getText c)))))
  (it "returns a button if input is an Action"
    (let [a (action :handler #(println "HI") :name "Test")
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

(describe to-document
  (it "returns nil if input is nil"
    (nil? (to-document nil)))
  (it "returns input if it's already a document"
    (let [d (javax.swing.text.PlainDocument.)]
      (expect (= d (to-document d)))))
  (it "returns the document of text component"
    (let [t (text)]
      (expect (= (.getDocument t) (to-document t))))))


(describe config!
  (it "configures the properties given to it on a single target"
    (let [p (JPanel.)
          result (config! p :foreground Color/RED :background Color/BLUE :enabled? false)]
      (expect (= p result))
      (expect (= Color/RED (.getForeground p)))
      (expect (= Color/BLUE (.getBackground p)))
      (expect (not (.isEnabled p)))))
  (it "configures the properties given to it on a multiple targets"
    (let [targets [(JPanel.) (JPanel.)]
          result (config! targets :foreground Color/RED :background Color/BLUE :enabled? false)]
      (expect (= targets result))
      (expect (= Color/RED (.getForeground (first targets))))
      (expect (= Color/BLUE (.getBackground (first targets))))
      (expect (not (.isEnabled (first targets))))
      (expect (= Color/RED (.getForeground (second targets))))
      (expect (= Color/BLUE (.getBackground (second targets))))
      (expect (not (.isEnabled (second targets))))))
  (it "configures a target with type-specific properties"
    (let [t (toggle :text "hi" :selected? false)]
      (expect (.isSelected (config! t :selected? true)))))
  (it "can configure an action"
    (let [a (action :name "foo")]
      (config! a :name "bar")
      (expect (= "bar" (.getValue a Action/NAME))))))

(describe flow-panel
  (it "should create a FlowLayout of :items list"
    (let [[a b c] [(JPanel.) (JPanel.) (JPanel.)]
          p (flow-panel :items [a b c] :align :trailing :hgap 99 :vgap 12 :align-on-baseline? true)
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

(describe text!
  (it "should set the text of a text Document"
    (let [d (javax.swing.text.PlainDocument.)
          _ (.insertString d 0 "HI" nil)
          r (text! d "BYE!")]
      (expect (= d r))
      (expect (= "BYE!" (text d))))))
  (it "should set the text of a single text widget argument"
    (= "BYE" (text (text! (text "HI") "BYE"))))
  (it "should set the text of a single button argument"
    (= "BYE" (text (text! (button :text "HI") "BYE"))))
  (it "should set the text of a seq of widget arguments"
    (let [[a b] [(text "HI") (text "BYE")]
          result (text! [a b] "YUM")]
      (expect (= [a b] result))
      (expect (= "YUM" (text a)))
      (expect (= "YUM" (text b)))))

(describe text
  (it "should throw IllegalArgumentException if argument is nil"
    (try
      (do (text nil) false)
      (catch IllegalArgumentException e true)))
  (it "should return the text of a single text widget argument"
    (= "HI" (text (text "HI"))))
  (it "should return the text of a text Document argument"
    (let [d (javax.swing.text.PlainDocument.)]
      (.insertString d 0 "HI" nil)
      (expect (= "HI" (text d)))))
  (it "should return the text of a button argument"
    (= "HI" (text (button :text "HI"))))
  (it "should return the text of a label argument"
    (= "HI" (text (label "HI"))))
  (it "should return the text of a seq of widget arguments"
    (= ["HI" "BYE"] (text [(text "HI") (button :text "BYE")])))
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
  (it "should default line wrapping to false"
    (not (.getLineWrap (text :multi-line? true))))
  (it "should enable line wrapping when :wrap-lines? is true"
    (.getLineWrap (text :multi-line? true :wrap-lines? true)))
  (it "should set tab size with :tab-size"
    (= 22 (.getTabSize (text :multi-line? true :tab-size 22))))
  (it "should set number of rows with :rows"
    (= 123 (.getRows (text :multi-line? true :rows 123))))
  (it "should honor the editable property"
    (let [t (text :text "HI" :editable? false :multi-line? true)]
      (expect (not (.isEditable t))))))

(describe editor-pane
  (it "should create a JEditorPane"
    (= javax.swing.JEditorPane (class (editor-pane)))))

(describe button
  (it "should create a JButton"
    (let [b (button :text "HI")]
      (expect (= JButton (class b)))
      (expect (= "HI" (.getText b)))))
  (it "should create a button from an action"
    (let [a (action :handler println)
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
      (.isSelected t))))

(describe listbox
  (it "should create a JList"
    (= javax.swing.JList (class (listbox))))
  (it "should create a JList using a seq as its model"
    (let [lb (listbox :model [1 2 3 4])
          model (.getModel lb)]
      (= [1 2 3 4] (map #(.getElementAt model %1) (range (.getSize model))))))
  (it "should set the list's cell renderer, if given"
    (let [render-fn (fn [renderer info] nil)
          renderer (default-list-cell-renderer render-fn)
          lb (listbox :renderer renderer)]
      (= renderer (.getCellRenderer lb)))))

(describe combobox
  (it "should create a JComboBox"
    (let [lb (combobox)]
      (= javax.swing.JComboBox (class lb))))
  (testing "the :editable? property"
    (it "should create a non-editable JComboBox when false"
      (not (.isEditable (combobox :editable? false))))
    (it "should create an editable JComboBox when true"
      (.isEditable (combobox :editable? true))))
  (it "should set the combobox's cell renderer, if given"
      (let [render-fn (fn [renderer info] nil)
            renderer (default-list-cell-renderer render-fn)
            lb (combobox :renderer renderer)]
        (= renderer (.getRenderer lb))))
  (it "should create a JComboBox using a seq as its model"
    (let [lb (combobox :model [1 2 3 4])
          model (.getModel lb)]
      (expect (= [1 2 3 4] (map #(.getElementAt model %1) (range (.getSize model)))))
      (expect (= 1 (.getSelectedItem model))))))

(describe table
  (it "should create a JTable"
    (= javax.swing.JTable (class (table))))
  (it "should fill viewport height by default"
    (.getFillsViewportHeight (table)))
  (it "should set the table's model from a TableModel"
    (let [m (javax.swing.table.DefaultTableModel.)
          t (table :model m)]
      (= m (.getModel t)))))

(describe tree
  (it "should create a JTree"
    (= javax.swing.JTree (class (tree))))
  (it "should set the tree's model from a TreeModel"
    (let [m (javax.swing.tree.DefaultTreeModel. nil)
          t (tree :model m)]
      (= m (.getModel t)))))

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

(describe menu-item
  (it "should create a JMenuItem"
      (expect (= javax.swing.JMenuItem (class (menu-item)))))
  (it "should create a menu item with an accelerator key"
    (let [ks (seesaw.keystroke/keystroke "ctrl S")
          mi (menu-item :key ks)]
      (expect (= ks (.getAccelerator mi)))))
  (it "should create a JMenuItem from an action"
    (let [a (action)
          mi (menu-item :action a)]
      (expect (= a (.getAction mi))))))

(describe menu
  (it "should create a JMenu"
    (expect (= javax.swing.JMenu (class (menu)))))
  (it "should create a JMenu with the given items"
    (let [a (action)
          b :separator
          c (menu-item)
          d "Just a string"
          m (menu :items [a b c d])
          [ia ib ic id] (.getMenuComponents m)]
      (expect (= a (.getAction ia)))
      (expect (= javax.swing.JSeparator (class ib)))
      (expect (= c ic))
      (expect (= "Just a string" (.getText id))))))

(describe popup
  (it "should create a JPopupMenu"
    (expect (= javax.swing.JPopupMenu (class (popup)))))
  (it "should create a JPopupMenu with the given items"
    (let [a (action)
          b :separator
          c (menu-item)
          d "Just a string"
          m (menu :items [a b c d])
          [ia ib ic id] (.getMenuComponents m)]
      (expect (= a (.getAction ia)))
      (expect (= javax.swing.JSeparator (class ib)))
      (expect (= c ic))
      (expect (= "Just a string" (.getText id))))))

(describe menubar
  (it "should create a JMenuBar"
    (= javax.swing.JMenuBar (class (menubar))))
  (it "should create a JMenuBar with the given items"
    (let [a (menu)
          b (menu)
          mb (menubar :items [a b])]
      (expect (= [a b] (vec (.getComponents mb)))))))

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

(describe canvas
  (it "should create a subclass of JPanel with no layout manager"
    (let [c (canvas)]
      (expect (instance? JPanel c))
      (expect (nil? (.getLayout c)))))
  (it "should call :before and :after functions given to the :paint property"
    (let [called (atom 0)
          before (fn [c g] (swap! called inc)) 
          after (fn [c g] (swap! called inc)) 
          c (canvas :paint { :before before :after after })]
      (.paintComponent c (.getGraphics (buffered-image 100 100))) ; fake with buffered image
      (expect (= 2 @called))))
  (it "should call a single function given to the :paint property"
    (let [called (atom 0)
          paint (fn [c g] (swap! called inc)) 
          c (canvas :paint paint)]
      (.paintComponent c (.getGraphics (buffered-image 100 100))) ; fake with buffered image
      (expect (= 1 @called)))))


(describe frame
  (it "should create a JFrame and set its title, width, and height"
    (let [f (frame :title "Hello" :width 99 :height 88 :visible? false)]
      (expect (= javax.swing.JFrame (class f)))
      (expect (= "Hello" (.getTitle f)))))
  (it "should set the frame's default close operation"
    (let [f (frame :visible? false :on-close :dispose)]
      (= javax.swing.JFrame/DISPOSE_ON_CLOSE (.getDefaultCloseOperation f))))
  (it "should create a JFrame and make is not resizable"
    (let [f (frame :title "Hello" :resizable? false :visible? false)]
      (expect (not (.isResizable f)))))
  (it "should create a JFrame and set its menu bar"
    (let [mb (menubar)
          f (frame :menubar mb :visible? false)]
      (expect (= mb (.getJMenuBar f)))))
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
  (it "should throw an exception if selector is not a vector"
    (try (do (select nil 99) false) (catch IllegalArgumentException e true)))
  (it "should find a widget by #id and returns it"
    (let [c (label :id "hi")
          p (flow-panel :id :panel :items [c])
          f (frame :title "select by id" :visible? false :content p)]
      (expect (= c (select f [:#hi])))
      (expect (= p (select f ["#panel"])))))
  (it "should find menu items by id in a frame's menubar"
    (let [m (menu-item :id :my-menu)
          f (frame :title "select menu item" :visible? false 
                   :menubar (menubar :items [(menu :text "File" :items [(menu :text "Nested" :items [m])])]))]
      (expect (= m (select f [:#my-menu]))))) 
  (it "should select all of the components in a tree with :*"
    (let [a (label) b (text) c (label)
          p (flow-panel :items [a b c])]
      (expect (= [p a b c] (select p [:*]))))))

