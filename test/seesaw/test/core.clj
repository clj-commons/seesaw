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
        seesaw.font
        seesaw.graphics
        seesaw.cells
        [seesaw.util :only (to-dimension)]
        [seesaw.color :only (color)])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]
        [clojure.string :only (capitalize split)])
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
  (it "cerces to a widget before getting the id"
    (let [b (button :id :my-button)
          e (java.awt.event.ActionEvent. b java.awt.event.ActionEvent/ACTION_PERFORMED "")]
      (expect (= "my-button" (id-for e)))))
  (it "returns the correct id if a widget has an id"
    (= "id of the label" (id-for (label :id "id of the label")))))

(defn invoke-getter [inst meth]
  (clojure.lang.Reflector/invokeInstanceMethod 
   inst
   meth
   (to-array [])))

(defmacro test-option [opt-kw initial-atom-value final-atom-value]
  `(testing ~(str "atom for option " (property-kw->java-name opt-kw))
     (it ~(str "should set the component's " (property-kw->java-name opt-kw) " using an atom")
       (let [~'a (atom ~initial-atom-value)
             ~'p (apply-default-opts (JPanel.) {~opt-kw ~'a})]
         (expect (= ~initial-atom-value (invoke-getter ~'p (property-kw->java-method ~opt-kw))))))
     (it ~(str "should update the " (property-kw->java-name opt-kw) " to value of atom")
       (let [~'a (atom ~initial-atom-value)
             ~'p (apply-default-opts (JPanel.) {~opt-kw ~'a})]
         (reset! ~'a ~final-atom-value)
         (expect (= ~final-atom-value (invoke-getter ~'p (property-kw->java-method ~opt-kw))))))
     (it ~(str "should update the atom to " (property-kw->java-name opt-kw))
       (let [~'a (atom ~initial-atom-value)
             ~'p (apply-default-opts (JPanel.) {~opt-kw ~'a})]
         (config! ~'p ~opt-kw ~final-atom-value)
         (expect (= ~final-atom-value ~'@a))))))

        
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
        (expect (= (Dimension. 10 20) (.getPreferredSize p)))))
    (test-option :preferred-size (to-dimension [10 :by 20]) (to-dimension [20 :by 20])))
  (testing "the :minimum-size option"
    (it "set the component's minimum size using to-dimension"
      (let [p (apply-default-opts (JPanel.) {:minimum-size [10 :by 20]})]
        (expect (= (Dimension. 10 20) (.getMinimumSize p)))))
    (test-option :minimum-size (to-dimension [10 :by 20]) (to-dimension [20 :by 20])))
  (testing "the :maximum-size option"
    (it "set the component's maximum size using to-dimension"
      (let [p (apply-default-opts (JPanel.) {:maximum-size [10 :by 20]})]
        (expect (= (Dimension. 10 20) (.getMaximumSize p)))))
    (test-option :maximum-size (to-dimension [10 :by 20]) (to-dimension [20 :by 20])))
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
    (it "sets the component's location with a two-element vector, where :* means keep the old value "
      (let [p (apply-default-opts (JPanel.) {:location [23 :*]})
            l (.getLocation p)]
        (expect (= [23 0] [(.x l) (.y l)]))))
    (it "sets the component's location with a java.awt.Point"
      (let [p (apply-default-opts (JPanel.) {:location (java.awt.Point. 23 45)})
            l (.getLocation p)]
        (expect (= [23 45] [(.x l) (.y l)]))))
    (it "sets the component's location with a java.awt.Rectangle"
      (let [p (apply-default-opts (JPanel.) {:location (java.awt.Rectangle. 23 45 99 100)})
            l (.getLocation p)]
        (expect (= [23 45] [(.x l) (.y l)])))))
  (testing "the :bounds option"
    (it "sets the component's bounds with a [x y width height] vector"
      (let [p (apply-default-opts (JPanel.) {:bounds [23 45 67 89]})
            b (.getBounds p)]
        (expect (= [23 45 67 89] [(.x b) (.y b) (.width b) (.height b)]))))
    (it "sets the component's bounds with a [x y width height] vector, where :* means keep the old value"
      (let [p (label :bounds [23 45 67 89])
            p (config! p :bounds [24 :* :* 90])
            b (.getBounds p)]
        (expect (= [24 45 67 90] [(.x b) (.y b) (.width b) (.height b)]))))
    (it "sets the component's bounds to its preferred size if given :preferred, preserving x and y"
      (let [p (label :bounds [23 45 67 89])
            ps (.getPreferredSize p)
            p (config! p :bounds :preferred)
            b (.getBounds p)]
        (expect (= [23 45 (.width ps) (.height ps)] [(.x b) (.y b) (.width b) (.height b)]))))
    (it "sets the component's bounds with a java.awt.Dimension, preserving x and y"
      (let [p (label :bounds [23 45 67 89])
            p (config! p :bounds (java.awt.Dimension. 80 90))
            b (.getBounds p)]
        (expect (= [23 45 80 90] [(.x b) (.y b) (.width b) (.height b)]))))
    (it "sets the component's bounds with a java.awt.Rectangle"
      (let [p (apply-default-opts (JPanel.) {:bounds (java.awt.Rectangle. 23 45 67 89)})
            b (.getBounds p)]
        (expect (= [23 45 67 89] [(.x b) (.y b) (.width b) (.height b)])))))

  (test-option :foreground (color 255 0 0) (color 0 0 0))
  (test-option :background (color 255 0 0) (color 0 0 0))
  ;; TODO: (test-option :border (color 255 0 0) (color 0 0 0))
  (test-option :font (font :name :monospaced) (font :name :serif))
  (test-option :tip "hello" "world")
  ;; TODO: (test-option :icon)
  ;; TODO: (test-option :action (action :name :blub) (action :name :bla))
  ;; TODO: (test-option :editable? true false)
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
  (testing "setting visible? option"
    (it "does nothing when omitted"
      (let [c (apply-default-opts (JPanel.))]
        (expect (.isVisible c))))
    (it "sets visible when provided"
      (let [c (apply-default-opts (JPanel.) {:visible? false})]
        (expect (not (.isVisible c)))))
    (it "sets visible when provided a truthy value"
      (let [c (apply-default-opts (JPanel.) {:visible? "something"})]
        (expect (.isVisible c))))
    (it "sets not visible when provided a falsey value"
      (let [c (apply-default-opts (JPanel.) {:visible? nil})]
        (expect (= false (.isVisible c)))))
    ;; TODO: (test-option :visible? false true) ;; for some reason no property change listener exists for this?
    )
  (testing "setting opaque? option"
    (it "does nothing when omitted"
      (let [c (apply-default-opts (JPanel.))]
        (expect (.isOpaque c))))
    (it "sets opacity when provided"
      (let [c (apply-default-opts (JPanel.) {:opaque? false})]
        (expect (not (.isOpaque c)))))
    (test-option :opaque? false true))
  (testing "the :model property"
    (it "sets the model when provided"
      (let [model  (javax.swing.DefaultButtonModel.)
            widget (apply-default-opts (button :model model))]
        (expect (= model (.getModel widget))))))
  (it "sets background using to-color when provided"
    (let [c (apply-default-opts (JPanel.) {:background "#000000" })]
      (expect (= Color/BLACK (.getBackground c)))))
  (it "sets opaque when background provided"
    (let [c (apply-default-opts (JLabel.) {:background "#000000" })]
      (expect (= true (.isOpaque c)))))
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
  (it "can configure a frame"
    (let [f (frame)]
      (config! f :title "I set the title")
      (expect (= "I set the title" (.getTitle f)))))
  (it "can configure a dialog"
    (let [f (dialog :visible? false)]
      (config! f :title "I set the title")
      (expect (= "I set the title" (.getTitle f)))))
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
      (expect (= [a b c] (seq (.getComponents p))))))
  (it "should throw IllegalArgumentException if a nil item is given"
    (try (flow-panel :items [nil]) false (catch IllegalArgumentException e true))))

(describe xyz-panel
  (it "should create a JPanel"
    (= JPanel (class (xyz-panel))))
  (it "should create a JPanel with a nil layout"
    (nil? (.getLayout (xyz-panel))))
  (it "should add items"
    (let [[a b c :as items] [(label :text "a") (label :text "b") (button :text "c")]
          p (xyz-panel :items items)]
      (expect (= items (vec (.getComponents p)))))))

(describe border-panel
  (it "should create a BorderLayout with given h and v gaps"
    (let [p (border-panel :hgap 99 :vgap 12)
          l (.getLayout p)]
      (expect (= java.awt.BorderLayout (class l)))
      (expect (= 99 (.getHgap l)))
      (expect (= 12 (.getVgap l)))))
  (it "should create a BorderLayout using direction options"
    (let [[n s e w c] [(JPanel.) (JPanel.) (JPanel.)(JPanel.)(JPanel.)]
          p (border-panel :hgap 99 :vgap 12 :north n :south s :east e :west w :center c)
          l (.getLayout p)]
      (expect (= java.awt.BorderLayout (class l)))
      (expect (= 99 (.getHgap l)))
      (expect (= 12 (.getVgap l)))
      (expect (= #{n s e w c} (apply hash-set (.getComponents p))))))
  (it "should create a BorderLayout using list of items with direction constraints"
    (let [[n s e w c] [(JPanel.) (JPanel.) (JPanel.)(JPanel.)(JPanel.)]
          p (border-panel :hgap 99 :vgap 12 :items [[n :north] [s :south][e :east][w :west][c :center]])
          l (.getLayout p)]
      (expect (= java.awt.BorderLayout (class l)))
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
  (it "should create an empty label"
    (let [l (label)]
      (expect (= JLabel (class l)))
      (expect (= "" (.getText l)))))
  (it "should create a label with tooltip"
    (expect (= "HI" (.getToolTipText (label :tip "HI")))))
  (it "should create a label with text when given a single argument"
    (expect (= "test label" (.getText (label "test label")))))
  (it "should create a label with text"
    (expect (= "test label" (.getText (label :text "test label")))))
  (it "should create a label with horizontal alignment"
    (= SwingConstants/LEFT (.getHorizontalAlignment (label :halign :left))))
  (it "should create a label with horizontal text position"
    (= SwingConstants/LEFT (.getHorizontalTextPosition (label :h-text-position :left))))
  (it "should create a label with vertical text position"
    (= SwingConstants/BOTTOM (.getVerticalTextPosition (label :v-text-position :bottom))))
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
  (it "should enable line wrapping on words when :wrap-lines? is true"
    (let [t (text :multi-line? true :wrap-lines? true)]
      (expect (.getLineWrap t))
      (expect (.getWrapStyleWord t))))
  (it "should set tab size with :tab-size"
    (= 22 (.getTabSize (text :multi-line? true :tab-size 22))))
  (it "should set number of rows with :rows"
    (= 123 (.getRows (text :multi-line? true :rows 123))))
  (it "should honor the editable property"
    (let [t (text :text "HI" :editable? false :multi-line? true)]
      (expect (not (.isEditable t))))))

(describe password
  (it "should create a JPasswordField"
    (= javax.swing.JPasswordField (class (password))))
  (it "should set the initial text"
    (= "secret" (text (password :text "secret"))))
  (it "should set the columns"
    (= 30 (.getColumns (password :columns 30))))
  (it "should set the echo char with a char"
    (= \S (.getEchoChar (password :echo-char \S)))))

(describe with-password*
  (it "should call the handler with the password in a character array"
    (let [s (atom nil)
          p (password :text "secret")]
      (with-password* p (fn [chars] (reset! s (apply str chars))))
      (expect (= "secret" @s))))

  (it "should return the return value of the handler"
    (= "HEY!" (with-password* (password) (fn [chars] "HEY!"))))

  (it "should fill the password character array with zeroes after then handler has completed"
    (let [s (atom nil)
          p (password :text "secret")]
      (with-password* p (fn [chars] (reset! s chars)))
      (expect (= [\0 \0 \0 \0 \0 \0] (vec @s))))))

(describe editor-pane
  (it "should create a JEditorPane"
    (= javax.swing.JEditorPane (class (editor-pane)))))

(describe button-group
  (it "should create a ButtonGroup"
    (instance? javax.swing.ButtonGroup (button-group)))
  (it "should create a button group with a list of buttons"
    (let [[a b c] [(radio) (checkbox) (toggle)]
          bg (button-group :buttons [a b c])]
      (expect (= [a b c] (enumeration-seq (.getElements bg)))))))

(describe button
  (it "should create a JButton"
    (let [b (button :text "HI")]
      (expect (= JButton (class b)))
      (expect (= "HI" (.getText b)))))

  (it "should add the button to a button group specified with the :group option"
    (let [bg (button-group)
          b  (button :group bg)]
      (expect (= b (first (enumeration-seq (.getElements bg)))))))

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
      (= m (.getModel t))))
  (it "should set the table's model using seesaw.table/table-model"
    (let [t (table :model [:columns [:a :b] :rows [[23 24] [25 26]]])
          m (.getModel t)]
      (expect (= 2 (.getRowCount m))))))

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
      (expect (= l (.. s getViewport getView)))))
  (it "should create a scroll pane with horizontal policy"
    (expect (= javax.swing.ScrollPaneConstants/HORIZONTAL_SCROLLBAR_NEVER (.getHorizontalScrollBarPolicy (scrollable (text) :hscroll :never)))))
  (it "should create a scroll pane with vertical policy"
    (expect (= javax.swing.ScrollPaneConstants/VERTICAL_SCROLLBAR_NEVER (.getVerticalScrollBarPolicy (scrollable (text) :vscroll :never)))))
  (it "should create a JScrollPane with options"
    (let [l (label :text "Test")
          s (scrollable l :id "MY-ID")]
      (expect (= "MY-ID" (id-for s))))))

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
  (it "should create a frame with an id"
    (= "my-frame" (id-for (frame :id :my-frame))))
  (it "should create a JFrame and set its title, width, and height"
    (let [f (frame :title "Hello" :width 99 :height 88)]
      (expect (= javax.swing.JFrame (class f)))
      (expect (= "Hello" (.getTitle f)))))
  (it "should set the frame's default close operation"
    (let [f (frame :on-close :dispose)]
      (= javax.swing.JFrame/DISPOSE_ON_CLOSE (.getDefaultCloseOperation f))))
  (it "should create a JFrame and make is not resizable"
    (let [f (frame :title "Hello" :resizable? false)]
      (expect (not (.isResizable f)))))
  (it "should create a JFrame and set its menu bar"
    (let [mb (menubar)
          f (frame :menubar mb)]
      (expect (= mb (.getJMenuBar f)))))
  (it "should create a JFrame and set its content pane"
    (let [c (label :text "HI")
          f (frame :content c)]
      (expect (= c (.getContentPane f))))))

(describe to-root
  (it "should convert a widget to its parent applet"
    (let [c (label :text "HI")
          a (javax.swing.JApplet.)]
      (.add a c)
      (expect (= a (to-root c)))))

  (it "should convert a widget to its parent frame"
    (let [c (label :text "HI")
          f (frame :content c)]
      (expect (= f (to-root c)))))
  (it "should return nil for an un-parented widget"
    (let [c (label :text "HI")]
      (expect (nil? (to-root c))))))

(describe to-frame
  (it "should be an alias for to-root"
    (= to-frame to-root)))

(letfn [(test-dlg-blocking
         [dlg & {:keys [future-fn] :or {future-fn #(Thread/sleep 100)}}]
         (let [v (atom nil)]
           (future
             (future-fn) 
             (swap! v #(if % % 'dialog-is-blocking))
             (invoke-now (.dispose dlg)))
           (invoke-now
            (let [r (show! dlg)] 
              (swap! v #(if % % r)))) 
           @v))]
  (describe custom-dialog
    (testing "argument passing"
      (it "should create a dialog with an id"
       (= "my-dialog" (id-for (custom-dialog :id :my-dialog :visible? false))))
     (it "should create a JDialog and set its title, width, and height"
       (let [f (custom-dialog :title "Hello" :width 99 :height 88 :visible? false)]
         (expect (= javax.swing.JDialog (class f)))
         (expect (= "Hello" (.getTitle f)))))
     (it "should set the dialog's default close operation"
       (let [f (custom-dialog :visible? false :on-close :dispose)]
         (= javax.swing.JDialog/DISPOSE_ON_CLOSE (.getDefaultCloseOperation f))))
     (it "should create a JDialog and make is not resizable"
       (let [f (custom-dialog :title "Hello" :resizable? false :visible? false)]
         (expect (not (.isResizable f)))))
     (it "should create a JDialog that is modal"
       (let [f (custom-dialog :title "Hello" :modal? true :visible? false)]
         (expect (.isModal f))))
     (it "should create a JDialog that is not modal"
       (let [f (custom-dialog :title "Hello" :modal? false :visible? false)]
         (expect (not (.isModal f)))))
     (it "should create a JDialog and set its menu bar"
       (let [mb (menubar)
             f (custom-dialog :menubar mb :visible? false)]
         (expect (= mb (.getJMenuBar f)))))
     (it "should create a JDialog and set its content pane"
       (let [c (label :text "HI")
             f (custom-dialog :content c :visible? false)]
         (expect (= c (.getContentPane f))))))
    (testing "blocking"
      (it "should block until dialog is being disposed of"
        (let [dlg (custom-dialog :visible? false :content "Nothing" :modal? true)]
          (expect (= (test-dlg-blocking dlg) 'dialog-is-blocking))))
      (it "should not block if :modal? is false"
        (let [dlg (custom-dialog :visible? false :content "Nothing" :modal? false)]
          (expect (= (test-dlg-blocking dlg) dlg))))
      (it "should return value passed to RETURN-FROM-DIALOG"
        (let [dlg (custom-dialog :visible? false :content "Nothing" :modal? true)]
          (expect (= (test-dlg-blocking
                      dlg :future-fn #(do
                                        (Thread/sleep 90)
                                        (return-from-dialog dlg :ok)
                                        (Thread/sleep 50))) :ok))))))

  
  (describe dialog
    (it "should block until dialog is being disposed of"
      (let [dlg (dialog :visible? false :content "Nothing" :modal? true)]
        (expect (= (test-dlg-blocking dlg) 'dialog-is-blocking))))
    (it "should not block"
      (let [dlg (dialog :visible? false :content "Nothing" :modal? false)]
        (expect (= (test-dlg-blocking dlg) dlg))))
    (testing "return-from-dialog"
      (let [ok (to-widget (action :name "Ok" :handler (fn [e] (return-from-dialog e :ok))) true)
            cancel (to-widget (action :name "Cancel" :handler (fn [e] (return-from-dialog e :cancel))) true)
            dlg (dialog :visible? false :content "Nothing"
                             :options (map #(to-widget % true) [ok cancel]))]
       (it "should return value passed to RETURN-FROM-DIALOG from clicking on ok button"
         (expect (= (test-dlg-blocking
                     dlg
                     :future-fn #(do
                                   (Thread/sleep 90)
                                   (invoke-now (.doClick ok))
                                   (Thread/sleep 50))) :ok)))
       (it "should return value passed to RETURN-FROM-DIALOG from clicking on cancel button"
         (expect (= (test-dlg-blocking
                     dlg
                     :future-fn #(do
                                   (Thread/sleep 90)
                                   (invoke-now (.doClick cancel))
                                   (Thread/sleep 50))) :cancel)))))))


(describe slider
  (it "should sync the value of the atom with the slider value, if slider value changed"
    (let [v (atom 15)
          sl (slider :value v)]
      (.setValue sl 20)
      (expect (= @v 20))))
  (it "should sync the value of the slider with the atom value, if atom value changed"
    (let [v (atom 15)
          sl (slider :value v)]
      (reset! v 20)
      (expect (= (.getValue sl) 20)))))

(describe progress-bar
  (it "should sync the value of the atom with the progress-bar value, if progress-bar value changed"
    (let [v (atom 15)
          pb (progress-bar :value v)]
      (.setValue pb 20)
      (expect (= @v 20))))
  (it "should sync the value of the progress-bar with the atom value, if atom value changed"
    (let [v (atom 15)
          pb (progress-bar :value v)]
      (reset! v 20)
      (expect (= (.getValue pb) 20)))))

(describe select
  (it "should throw an exception if selector is not a vector"
    (try (do (select nil 99) false) (catch IllegalArgumentException e true)))
  (it "should find a frame by #id and return it"
    (let [f (frame :id :my-frame)]
      (expect (= f (select f [:#my-frame])))))
  (it "should find a widget by #id and returns it"
    (let [c (label :id "hi")
          p (flow-panel :id :panel :items [c])
          f (frame :title "select by id" :content p)]
      (expect (= c (select f [:#hi])))
      (expect (= p (select f ["#panel"])))))
  (it "should find menu items by id in a frame's menubar"
    (let [m (menu-item :id :my-menu)
          f (frame :title "select menu item"
                   :menubar (menubar :items [(menu :text "File" :items [(menu :text "Nested" :items [m])])]))]
      (expect (= m (select f [:#my-menu]))))) 
  (it "should select all of the components in a tree with :*"
    (let [a (label) b (text) c (label)
          p (flow-panel :items [a b c])]
      (expect (= [p a b c] (select p [:*]))))))

(describe add!
  (testing "When called on a panel with a FlowLayout"
    (it "adds a widget to the end of the panel"
      (let [p (flow-panel)
            l (label)
            result (add! p l)]
        (expect (= result p))
        (expect (= l (first (.getComponents p))))))
    (it "adds a widget to the end of the panel"
      (let [p (flow-panel)
            label0 (label)
            label1 (label)
            result (add! p [label0 nil] label1 )]
        (expect (= result p))
        (expect (= label0 (first (.getComponents p))))
        (expect (= label1 (second (.getComponents p)))))))

  (testing "When called on a panel with a BoxLayout"
    (it "adds a widget to the end of the panel"
      (let [p (vertical-panel)
            l (label)
            result (add! p l)]
        (expect (= result p))
        (expect (= l (first (.getComponents p)))))))

  (testing "When called on a panel with a BorderLayout"
    (it "adds a widget at the given location"
      (let [p (border-panel)
            l (label)
            result (add! p [l :north])]
        (expect (= result p))
        (expect (= BorderLayout/NORTH (.getConstraints (.getLayout p) l)))))))

(describe remove!
  (it "removes widgets from a container"
    (let [l0 (label) l1 (label)
          p (border-panel :north l0 :south l1)
          result (remove! p l0 l1)]
      (expect (= p result))
      (expect (= 0 (count (.getComponents p)))))))

(describe replace!
  (testing "when called on a panel with a generic layout (e.g. flow)"
    (it "replaces the given widget with a new widget"
      (let [l0 (label "l0")
            l1 (label "l1")
            l2 (label "l2")
            p (flow-panel :items [l0 l1])
            result (replace! p l1 l2)]
        (expect (= p result))
        (expect (= [l0 l2] (vec (.getComponents p)))))))
  (testing "when called on a panel with a border layout"
    (it "replaces the given widget with a new widget and maintains constraints"
      (let [l0 (label "l0")
            l1 (label "l1")
            l2 (label "l2")
            p (border-panel :north l0 :south l1)
            result (replace! p l1 l2)]
        (expect (= p result))
        (expect (= [l0 l2] (vec (.getComponents p))))
        (expect (= BorderLayout/SOUTH (-> p .getLayout (.getConstraints l2)))))))
  (testing "when called on a panel with a mid layout"
    (it "replaces the given widget with a new widget and maintains constraints"
      (let [l0 (label "l0")
            l1 (label "l1")
            l2 (label "l2")
            p (mig-panel :items [[l0 ""] [l1 "wrap"]])
            result (replace! p l1 l2)]
        (expect (= p result))
        (expect (= [l0 l2] (vec (.getComponents p))))
        (expect (= "wrap" (-> p .getLayout (.getComponentConstraints l2))))))))

(describe selection
  (it "should get the selection from a button-group"
    (let [a (radio)
          b (radio :selected? true)
          bg (button-group :buttons [a b])]
      (expect (= b (selection bg))))))

(describe selection!
  (it "should set the selection of a button-group"
    (let [a (radio)
          b (radio)
          bg (button-group :buttons [a b])]
      (expect (nil? (selection bg)))
      (selection! bg b)
      (expect (= b (selection bg))))))

(describe with-widget
  (it "throws an exception if the factory class does not create the expected type"
    (try
      (do (with-widget JPanel (text :id :hi)) false)
      (catch IllegalArgumentException e (.contains (.getMessage e) "is not an instance of"))))
  (it "throws an exception if the factory function does not create the expected type"
    (try
      (do (with-widget #(JPanel.) (text :id :hi)) false)
      (catch IllegalArgumentException e (.contains (.getMessage e) "is not an instance of"))))
  (it "throws an exception if the given instance is not the expected type"
    (try
      (do (with-widget (JPanel.) (text :id :hi)) false)
      (catch IllegalArgumentException e (.contains (.getMessage e) "is not consistent with expected type"))))
  (it "uses a function as a factory and applies a constructor function to the result"
    (let [expected (javax.swing.JPasswordField.)
          result   (with-widget (fn [] expected) (text :id "hi"))]
      (expect (= expected result))
      (expect (= "hi" (id-for result)))))
  (it "uses a class literal as a factory and applies a constructor function to it"
    (let [result (with-widget javax.swing.JPasswordField (text :id "hi"))]
      (expect (instance? javax.swing.JPasswordField result))
      (expect (= "hi" (id-for result)))))
  (it "applies a constructor function to an existing widget instance"
    (let [existing (JPanel.)
          result (with-widget existing (border-panel :id "hi"))]
      (expect (= existing result))
      (expect (= "hi" (id-for existing))))))

(describe dispose!
  (it "should dispose of a JFrame"
    (let [f (pack! (frame :title "dispose!"))]
      (expect (.isDisplayable f))
      (let [result (dispose! f)]
        (expect (= result f))
        (expect (not (.isDisplayable f))))))
  (it "should dispose of a JDialog"
    (let [f (dialog :title "dispose!" :visible? false)]
      (expect (.isDisplayable f))
      (let [result (dispose! f)]
        (expect (= result f))
        (expect (not (.isDisplayable f)))))))

(describe assert-ui-thread
  ; TODO test non-exception case
  (it "should throw an IllegalStateException if not called on the Swing UI thread"
     @(future
        (try 
          (do (assert-ui-thread "some message") false)
          (catch IllegalStateException e true)))))

(describe move!
  (it "should move the widget to the back of the z order"
      (let [a (label)
            b (label)
            p (xyz-panel :items [a b])]
        (expect (= 0 (.getComponentZOrder p a)))
        (move! a :to-back)
        (expect (= 1 (.getComponentZOrder p a)))))
  (it "should move the widget to the front of the z order"
      (let [a (label)
            b (label)
            p (xyz-panel :items [a b])]
        (expect (= 1 (.getComponentZOrder p b)))
        (move! b :to-front)
        (expect (= 0 (.getComponentZOrder p b)))))
  (it "should set the absolute location of a widget with a vector"
      (let [lbl (label)
            point [101 102]
            result (move! lbl :to point)
            new-loc (.getLocation lbl)]
        (expect (= (java.awt.Point. 101 102) new-loc))))
  (it "should set the absolute location of a widget with a vector, where :* means to keep the old value"
      (let [lbl (label :location [5 6])
            point [:* 102]
            result (move! lbl :to point)
            new-loc (.getLocation lbl)]
        (expect (= (java.awt.Point. 5 102) new-loc))))
  (it "should set the absolute location of a widget with a Point"
    (let [lbl (label)
          point (java.awt.Point. 99 100)
          result (move! lbl :to point)
          new-loc (.getLocation lbl)]
      (expect (= point new-loc))))
  (it "should set the absolute location of a widget with the upper left corner of a Rectangle"
    (let [lbl (label)
          point (java.awt.Rectangle. 99 100 123 456)
          result (move! lbl :to point)
          new-loc (.getLocation lbl)]
      (expect (= (.getLocation point) new-loc))))
  (it "should set the relative location of a widget with a vector"
      (let [lbl (label)
            point [101 102]
            _ (move! lbl :to [5 40])
            result (move! lbl :by point)
            new-loc (.getLocation lbl)]
        (expect (= (java.awt.Point. 106 142) new-loc))))
  (it "should set the relative location of a widget with a Point"
    (let [lbl (label)
          point (java.awt.Point. 99 100)
          _ (move! lbl :to [5 40])
          result (move! lbl :by point)
          new-loc (.getLocation lbl)]
      (expect (= (java.awt.Point. 104 140) new-loc)))))

