;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.core
  (:require [seesaw.selector :as selector]
            [seesaw.cursor :as cursor]
            clojure.java.io)
  (:use seesaw.core
        seesaw.font
        seesaw.graphics
        seesaw.cells
        [seesaw.util :only (to-dimension children)]
        [seesaw.color :only (color)])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]
        [clojure.string :only (capitalize split)])
  (:import [javax.swing SwingConstants
                        ScrollPaneConstants
                        Action
                        JFrame
                        JToolBar JTabbedPane
                        JPanel JLabel JButton JTextField JTextArea Box Box$Filler BoxLayout JTextPane
                        JToggleButton JCheckBox JRadioButton
                        JScrollPane
                        JSplitPane]
           [javax.swing.text StyleConstants]
           [java.awt Insets Color Dimension FlowLayout BorderLayout]
           [java.awt.event ActionEvent]))

(describe id-of
  (it "returns nil if a widget doesn't have an id"
    (nil? (id-of (label))))
  (it "coerces to a widget before getting the id"
    (let [b (button :id :my-button)
          e (java.awt.event.ActionEvent. b java.awt.event.ActionEvent/ACTION_PERFORMED "")]
      (expect (= :my-button (id-of e)))))
  (it "returns the correct id, as a keyword, if a widget has an id"
    (= (keyword "id of the label") (id-of (label :id "id of the label")))))

(describe "Applying default options" 
  (testing "the :id option"
    (it "does nothing when omitted"
      (expect (nil? (-> (JPanel.) apply-default-opts id-of))))
    (it "sets the component's id as a keyword if given"
      (expect (= :hi (-> (JLabel.) (apply-default-opts {:id "hi"}) id-of))))
    (it "throws IllegalStateException if the widget's id is already set"
      (try 
        (do (config! (label :id :foo) :id :bar) false)
        (catch IllegalStateException e true))))

  (testing "the :class option"
    (it "does nothing when omitted"
      (expect (nil? (-> (JPanel.) apply-default-opts selector/class-of))))
    (it "sets the class of the widget"
      (expect (= #{"foo"} (selector/class-of (flow-panel :class :foo)))))
    (it "sets the classes of a widget"
      (expect (= #{"foo" "bar"} (selector/class-of (flow-panel :class #{:foo :bar}))))))

  (testing "the :focusable? option"
    (it "makes a widget focusable"
      (.isFocusable (label :text "focusable" :focusable? true))))

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

  (testing "the :cursor option"
    (it "sets the widget's cursor when given a cursor"
      (let [c (cursor/cursor :hand)
            p (apply-default-opts (JPanel.) {:cursor c})]
        (expect (= c (.getCursor p)))))
    (it "sets the widget's cursor when given a cursor type keyword"
      (let [p (apply-default-opts (JPanel.) {:cursor :hand})]
        (expect (= java.awt.Cursor/HAND_CURSOR (.getType (.getCursor p)))))))

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
        (expect (= false (.isVisible c))))))

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
  (it "sets opaque when background provided"
    (let [c (apply-default-opts (JLabel.) {:background "#000000" })]
      (expect (= true (.isOpaque c)))))
  (it "sets foreground when provided"
    (let [c (apply-default-opts (JPanel.) {:foreground "#00FF00" })]
      (expect (= Color/GREEN (.getForeground c)))))
  (it "sets border when provided using to-border"
    (let [c (apply-default-opts (JPanel.) {:border "TEST"})]
      (expect (= "TEST" (.. c getBorder getTitle)))))
  (it "sets cursor when provided"
    (let [c (apply-default-opts (JPanel.) {:cursor :hand})]
      (expect (= java.awt.Cursor/HAND_CURSOR (.getType (.getCursor c)))))))

(describe show!
  (it "makes a widget visible and returns it"
    (.isVisible (show! (doto (JPanel.) (.setVisible false))))))

(describe hide!
  (it "hides a widget and returns it"
    (not (.isVisible (hide! (doto (JPanel.) (.setVisible true)))))))

(describe make-widget
  (it "throws an exception for unsupported arguments"
    (try (make-widget 99) false (catch Exception e true)))
  (it "returns nil if input is nil"
    (= nil (make-widget nil)))
  (it "returns input if it's already a widget"
    (let [c (JPanel.)]
      (expect (= c (make-widget c)))))
  (it "returns input if it's a JFrame"
    (let [c (JFrame.)]
      (expect (= c (make-widget c)))))
  (it "returns a label for string input"
    (let [c (make-widget "TEST")]
      (expect (= "TEST" (.getText c)))))
  (it "returns a button if input is an Action"
    (let [a (action :handler #(println "HI") :name "Test")
          c (make-widget a)]
      (expect (isa? (class c) javax.swing.JButton))
      (expect (= "Test" (.getText c)))))
  (it "creates a separator for :separator"
    (expect (= javax.swing.JSeparator (class (make-widget :separator)))))
  (it "creates horizontal glue for :fill-h"
    (let [c (make-widget :fill-h)]
      (expect (isa? (class c) javax.swing.Box$Filler ))
      (expect (= 32767 (.. c getMaximumSize getWidth)))))
  (it "creates vertical glue for :fill-v"
    (let [c (make-widget :fill-v)]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 32767 (.. c getMaximumSize getHeight)))))
  (it "creates a vertical strut for [:fill-v N]"
    (let [c (make-widget [:fill-v 99])]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 32767 (.. c getMaximumSize getWidth)))
      (expect (= 99 (.. c getMaximumSize getHeight)))
      (expect (= 99 (.. c getPreferredSize getHeight)))))
  (it "creates a horizontal strut for [:fill-h N]"
    (let [c (make-widget [:fill-h 88])]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 32767 (.. c getMaximumSize getHeight)))
      (expect (= 88 (.. c getMaximumSize getWidth)))
      (expect (= 88 (.. c getPreferredSize getWidth)))))
  (it "creates a rigid area for a Dimension"
    (let [c (make-widget (Dimension. 12 34))]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 12 (.. c getMaximumSize getWidth)))
      (expect (= 34 (.. c getMaximumSize getHeight)))
      (expect (= 12 (.. c getPreferredSize getWidth)))
      (expect (= 34 (.. c getPreferredSize getHeight)))))
  (it "creates a rigid area for a [N :by N]"
    (let [c (make-widget [12 :by 34])]
      (expect (isa? (class c) javax.swing.Box$Filler))
      (expect (= 12 (.. c getMaximumSize getWidth)))
      (expect (= 34 (.. c getMaximumSize getHeight)))
      (expect (= 12 (.. c getPreferredSize getWidth)))
      (expect (= 34 (.. c getPreferredSize getHeight))))))

(describe to-widget
  (it "returns nil for unknown inputs"
      (= nil (to-widget "a string")))
  (it "returns nil if input is nil"
      (= nil (to-widget nil)))
  (it "returns input if it's already a widget"
    (let [c (JPanel.)]
      (expect (= c (to-widget c)))))
  (it "returns input if it's a JFrame"
    (let [c (JFrame.)]
      (expect (= c (to-widget c)))))
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

(defmacro verify-config [target key getter]
  (let [t (gensym "t")] 
    `(it ~(str "can retrieve the value of " key " from a widget") 
     (let [~t ~target
           expected# ~(if (symbol? getter) `(. ~t ~getter) getter)
           actual# (config ~t ~key)]
       (expect (= expected# actual#))))))

(describe config
  (it "throws IllegalArgumentException for an unknown option"
    (try
      (config (text "HI") :textish)
      false
      (catch IllegalArgumentException e true)))
  (it "can retrieve the :id of a widget"
    (= :foo (config (text :id :foo) :id)))
  (it "can retrieve the :class of a widget"
    (= #{"foo"} (config (text :class :foo) :class)))
  (verify-config (text :text "HI") :text "HI")
  (verify-config (button :text "button") :text "button")
  (verify-config (label :text "label") :text "label")
  (verify-config (text :opaque? false) :opaque? false)
  (verify-config (text :opaque? true) :opaque? true)
  (verify-config (text) :enabled? isEnabled)
  (verify-config (text :size [100 :by 101]) :size getSize)
  (verify-config (text :preferred-size [100 :by 101]) :preferred-size getPreferredSize)
  (verify-config (text :minimum-size [100 :by 101]) :minimum-size getMinimumSize)
  (verify-config (text :maximum-size [100 :by 101]) :maximum-size getMaximumSize)
  (verify-config (text) :foreground getForeground)
  (verify-config (text) :background getBackground)
  (verify-config (text :focusable? true) :focusable? true)
  (verify-config (text :focusable? false) :focusable? false)
  (verify-config (text :visible? true) :visible? true)
  (verify-config (text :visible? false) :visible? false)
  (verify-config (border-panel :border 1) :border getBorder)
  (verify-config (border-panel :location [100 200]) :location (java.awt.Point. 100 200))
  (verify-config (border-panel :bounds [100 200 300 400]) :bounds (java.awt.Rectangle. 100 200 300 400))
  (verify-config (border-panel :font :monospace) :border getBorder)
  (verify-config (border-panel :tip "A tool tip") :tip "A tool tip")
  (verify-config (border-panel :cursor :hand) :cursor getCursor)
  (verify-config (button) :model getModel)
  (verify-config (text) :model getDocument)
  (verify-config (combobox) :model getModel)
  (verify-config (listbox) :model getModel)
  (verify-config (table) :model getModel)
  (verify-config (tree) :model getModel)
  (verify-config (progress-bar) :model getModel)
  (verify-config (slider) :model getModel)
          )

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
    (let [f (dialog)]
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
  (it "should returns :items with config"
    (let [items [(JPanel.) (JPanel.) (JPanel.)]
          p     (flow-panel :items items)]
      (expect (= items (config p :items)))))
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
      (expect (= #{n s e w c} (apply hash-set (.getComponents p))))))
  (it "should return its :items with config"
    (let [[n s e w c] [(JPanel.) (JPanel.) (JPanel.)(JPanel.)(JPanel.)]
           items [[n :north] [s :south][e :east][w :west][c :center]]
          p (border-panel :hgap 99 :vgap 12 :items items)]
      (expect (= items (config p :items))))))

(describe horizontal-panel
  (it "should create a horizontal box of :items list"
    (let [[a b c] [(JPanel.) (JPanel.) (JPanel.)]
          p (horizontal-panel :items [a b c])]
      (expect (= BoxLayout/X_AXIS (.. p getLayout getAxis)))
      (expect (= [a b c] (seq (.getComponents p))))))
  (it "should get :items with config"
    (let [items [(JPanel.) (JPanel.) (JPanel.)]
          p (horizontal-panel :items items)]
      (expect (= items (config p :items))))))

(describe vertical-panel
  (it "should create a vertical box of :items list"
    (let [[a b c] [(JPanel.) (JPanel.) (JPanel.)]
          p (vertical-panel :items [a b c])]
      (expect (= BoxLayout/Y_AXIS (.. p getLayout getAxis)))
      (expect (= [a b c] (seq (.getComponents p))))))
  (it "should get :items with config"
    (let [items [(JPanel.) (JPanel.) (JPanel.)]
          p (vertical-panel :items items)]
      (expect (= items (config p :items))))))

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
      (expect (= [a b c] (seq (.getComponents g))))))
  (it "should get :items with config"
    (let [items [(label :text "A") (label :text "B") (label :text "C")]
          g (grid-panel :items items)] 
      (expect (= items (config g :items))))))

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
    (let [p (form-panel :items [["hi" :weighty 999 :gridwidth 1]])
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
  (it "should set the text of the document in a document event"
    (let [doc (javax.swing.text.PlainDocument.)
          evt (javax.swing.text.AbstractDocument$DefaultDocumentEvent. doc 0 0 
                                                 javax.swing.event.DocumentEvent$EventType/CHANGE)]
      (text! evt "Hello")
      (expect (= "Hello" (text evt)))))
  (it "should set the text of a text Document"
    (let [d (javax.swing.text.PlainDocument.)
          _ (.insertString d 0 "HI" nil)
          r (text! d "BYE!")]
      (expect (= d r))
      (expect (= "BYE!" (text d)))))
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
  (it "should set the text of a widget to an integer"
    (= "99" (text (text! (text "initial") 99))))
  (it "should set the text of a widget to a double"
    (= (str 3.14) (text (text! (text "initial") 3.14))))
  (it "should set the text of a widget to a rational"
    (= (str 1/3) (text (text! (text "initial") 1/3))))
  (it "should set the text of a widget to the contents of a non-string 'slurpable'"
    (let [t (text :multi-line? true)]
      (text! t (clojure.java.io/resource "seesaw/test/core.text.txt"))
      ; Be careful editing the test file with vim. It will silently add
      ; a trailing newline on save.
      (expect (= "Some text in a resource" (text t)))))) 

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
  (it "should return the text of the document in a document event"
    (let [doc (javax.swing.text.PlainDocument.)
          evt (javax.swing.text.AbstractDocument$DefaultDocumentEvent. doc 0 0 
                                                 javax.swing.event.DocumentEvent$EventType/CHANGE)]
      (.insertString doc 0 "Hello" nil)
      (expect (= "Hello" (text evt)))))
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
  (it "should create a text field with the given :columns"
    (let [t (text :text "HI" :columns 55)]
      (expect (= 55 (.getColumns t)))))
  (it "should create a text area when multi-line? is true"
    (let [t (text :text "HI" :multi-line? true)]
      (expect (= JTextArea (class t)))
      (expect (= "HI" (.getText t)))))
  (it "should create a text area with the given :columns"
    (let [t (text :text "HI" :multi-line? true :columns 91)]
      (expect (= 91 (.getColumns t)))))
  (it "should default line wrapping to false"
    (not (.getLineWrap (text :multi-line? true))))
  (it "should enable line wrapping on words when :wrap-lines? is true"
    (let [t (text :multi-line? true :wrap-lines? true)]
      (expect (.getLineWrap t))
      (expect (.getWrapStyleWord t))))
  (verify-config (text :multi-line? true :wrap-lines? true) :wrap-lines? true)
  (it "should set tab size with :tab-size"
    (= 22 (.getTabSize (text :multi-line? true :tab-size 22))))
  (it "should set number of rows with :rows"
    (= 123 (.getRows (text :multi-line? true :rows 123))))
  (it "should set the :caret-color"
    (= Color/ORANGE (.getCaretColor (text :caret-color Color/ORANGE))))
  (it "should set the :disabled-text-color"
    (= Color/ORANGE (.getDisabledTextColor (text :disabled-text-color Color/ORANGE))))
  (it "should set the :selected-text-color"
    (= Color/ORANGE (.getSelectedTextColor (text :selected-text-color Color/ORANGE))))
  (it "should set the :selection-color"
    (= Color/ORANGE (.getSelectionColor (text :selection-color Color/ORANGE))))
  (it "should handle the :margin option with to-insets"
    (let [t (text :margin 1)
          i   (.getMargin t)]
      (expect (= [1 1 1 1] [(.top i) (.left i) (.bottom i) (.right i)]))))
  (it "should honor the editable property"
    (let [t (text :text "HI" :editable? false :multi-line? true)]
      (expect (not (.isEditable t))))))

(describe styled-text
  (it "should create a text pane"
    (let [t (styled-text :text "HI")]
      (expect (instance? JTextPane t))
      (expect (= "HI" (text t)))))
  (verify-config (styled-text :wrap-lines? true) :wrap-lines? true)
  (verify-config (styled-text :wrap-lines? false) :wrap-lines? false)
  (it "should add styles"
    (let [t (styled-text :text "HI" 
                    :styles [[:big :size 30]
                            [:small :size 3]])
          style (.getStyle t "big")] 
      (expect (isa? (class style) javax.swing.text.Style))
      (expect (.containsAttribute style StyleConstants/FontSize 30)))))

(describe style-text!
  (let [t (styled-text :text "HI"
                       :styles [[:big :size 30]
                                [:small :size 3]])]
    (it "should style the text"
        (expect (= t (style-text! t :big 0 2)))
        (expect (.containsAttribute (.getCharacterAttributes t) 
                                    StyleConstants/FontSize 30)))))

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
      (expect (= [a b c] (enumeration-seq (.getElements bg))))))
  (it "should return buttons in the group with config :buttons"
    (let [buttons [(radio) (checkbox) (toggle)]
          bg (button-group :buttons buttons)]
      (expect (= buttons (config bg :buttons)))
      ;(expect (= [a b c] (enumeration-seq (.getElements bg))))
      )))

(describe button
  (it "should create a JButton"
    (let [b (button :text "HI")]
      (expect (= JButton (class b)))
      (expect (= "HI" (.getText b)))))
  (it "should handle the :margin option with to-insets"
    (let [b (button :margin 1)
          i   (.getMargin b)]
      (expect (= [1 1 1 1] [(.top i) (.left i) (.bottom i) (.right i)]))))


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
  (it "should create a JList with :fixed-cell-height set"
    (= 98 (.getFixedCellHeight (listbox :fixed-cell-height 98))))
  (it "should create a JList and set the selection mode"
    (expect (= javax.swing.ListSelectionModel/SINGLE_SELECTION (.getSelectionMode (listbox :selection-mode :single)))))
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
  (it "should create a JTable with :single selection-mode set"
    (= javax.swing.ListSelectionModel/SINGLE_SELECTION (.. (table :selection-mode :single) getSelectionModel getSelectionMode)))
  (it "should create a JTable with :multi-interval selection-mode set"
    (= javax.swing.ListSelectionModel/MULTIPLE_INTERVAL_SELECTION (.. (table :selection-mode :multi-interval) getSelectionModel getSelectionMode)))
  (it "should fill viewport height by default"
    (.getFillsViewportHeight (table)))
  (it "should set the table's model from a TableModel"
    (let [m (javax.swing.table.DefaultTableModel.)
          t (table :model m)]
      (= m (.getModel t))))
  (it "should set the table's model using seesaw.table/table-model"
    (let [t (table :model [:columns [:a :b] :rows [[23 24] [25 26]]])
          m (.getModel t)]
      (expect (= 2 (.getRowCount m)))))
  (verify-config (table :fills-viewport-height? true) :fills-viewport-height? true)
  (verify-config (table :fills-viewport-height? false) :fills-viewport-height? false)
  (verify-config (table :show-grid? true) :show-grid? true)
  (verify-config (table :show-grid? false) :show-grid? false)
  (verify-config (table :show-vertical-lines? true) :show-vertical-lines? true)
  (verify-config (table :show-vertical-lines? false) :show-vertical-lines? false)
  (verify-config (table :show-horizontal-lines? true) :show-horizontal-lines? true)
  (verify-config (table :show-horizontal-lines? false) :show-horizontal-lines? false)
  (it "should honor :auto-resize :off"
    (= javax.swing.JTable/AUTO_RESIZE_OFF (.getAutoResizeMode (table :auto-resize :off))))
  (it "should honor :auto-resize :next-column"
    (= javax.swing.JTable/AUTO_RESIZE_NEXT_COLUMN (.getAutoResizeMode (table :auto-resize :next-column))))
  (it "should honor :auto-resize :subsequent-columns"
    (= javax.swing.JTable/AUTO_RESIZE_SUBSEQUENT_COLUMNS (.getAutoResizeMode (table :auto-resize :subsequent-columns))))
  (it "should honor :auto-resize :last-column"
    (= javax.swing.JTable/AUTO_RESIZE_LAST_COLUMN (.getAutoResizeMode (table :auto-resize :last-column))))
  (it "should honor :auto-resize :all-columns"
    (= javax.swing.JTable/AUTO_RESIZE_ALL_COLUMNS (.getAutoResizeMode (table :auto-resize :all-columns)))))

(describe tree
  (it "should create a JTree"
    (= javax.swing.JTree (class (tree))))
  (verify-config (tree :expands-selected-paths? true) :expands-selected-paths? true)
  (verify-config (tree :expands-selected-paths? false) :expands-selected-paths? false)
  (verify-config (tree :scrolls-on-expand? true) :scrolls-on-expand? true)
  (verify-config (tree :scrolls-on-expand? false) :scrolls-on-expand? false)
  (verify-config (tree :shows-root-handles? true) :shows-root-handles? true)
  (verify-config (tree :shows-root-handles? false) :shows-root-handles? false)
  (verify-config (tree :toggle-click-count 2) :toggle-click-count 2)
  (verify-config (tree :toggle-click-count 1) :toggle-click-count 1)
  (verify-config (tree :visible-row-count 20) :visible-row-count 20)
  (it "should create a JTree with :root-visible? true"
    (.isRootVisible (tree :root-visible? true)))
  (it "should create a JTree with :root-visible? false"
    (not (.isRootVisible (tree :root-visible? false))))
  (it "should create a JTree with :shows-root-handles? true"
    (.getShowsRootHandles (tree :shows-root-handles? true)))
  (it "should create a JTree with :shows-root-handles? false"
    (not (.getShowsRootHandles (tree :shows-root-handles? false))))
  (it "should create a JTree with :single selection-mode set"
    (= javax.swing.tree.TreeSelectionModel/SINGLE_TREE_SELECTION (.. (tree :selection-mode :single) getSelectionModel getSelectionMode)))
  (it "should create a JTree with :discontiguous selection-mode set"
    (= javax.swing.tree.TreeSelectionModel/DISCONTIGUOUS_TREE_SELECTION (.. (tree :selection-mode :discontiguous) getSelectionModel getSelectionMode)))
  (it "should create a JTree with :contiguous selection-mode set"
    (= javax.swing.tree.TreeSelectionModel/CONTIGUOUS_TREE_SELECTION (.. (tree :selection-mode :contiguous) getSelectionModel getSelectionMode)))
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
    (expect (= ScrollPaneConstants/HORIZONTAL_SCROLLBAR_NEVER (.getHorizontalScrollBarPolicy (scrollable (text) :hscroll :never)))))
  (it "should create a scroll pane with vertical policy"
    (expect (= ScrollPaneConstants/VERTICAL_SCROLLBAR_NEVER (.getVerticalScrollBarPolicy (scrollable (text) :vscroll :never)))))
  (it "should create a JScrollPane with a :row-header-view"
    (let [hv (label)
          s (scrollable (button) :row-header hv)]
      (expect (= hv (.. s getRowHeader getView)))))
  (it "should create a JScrollPane with a :column-header-view"
    (let [hv (label)
          s (scrollable (button) :column-header hv)]
      (expect (= hv (.. s getColumnHeader getView)))))
  (it "should create a JScrollPane with corners"
    (let [[ll lr ul ur :as corners] [(label) (label) (label) (label)]
          s (scrollable (button) :lower-left ll :lower-right lr :upper-left ul :upper-right ur)]
      (expect (= corners [(.getCorner s ScrollPaneConstants/LOWER_LEFT_CORNER)
                          (.getCorner s ScrollPaneConstants/LOWER_RIGHT_CORNER)
                          (.getCorner s ScrollPaneConstants/UPPER_LEFT_CORNER)
                          (.getCorner s ScrollPaneConstants/UPPER_RIGHT_CORNER)]))))
  (it "should create a JScrollPane with options"
    (let [l (label :text "Test")
          s (scrollable l :id "MY-ID")]
      (expect (= (keyword "MY-ID") (id-of s))))))

(describe splitter
  (it "should create a JSplitPane with with two panes"
    (let [left (label :text "Left")
          right (label :text "Right")
          s (splitter :left-right left right)]
      (expect (= javax.swing.JSplitPane (class s)))
      (expect (= left (.getLeftComponent s)))
      (expect (= right (.getRightComponent s)))))
  (verify-config (splitter :top-bottom "top" "bottom" :divider-location 99) :divider-location 99)
  (it "should set the divider location to an absolute pixel location with an int"
    (let [s (splitter :top-bottom "top" "bottom" :divider-location 99)]
      (expect (= 99 (.getDividerLocation s)))))
  (it "should set the divider location to a percentage location with a double (eventually)"
    (let [s (splitter :top-bottom "top" "bottom" :divider-location 0.5)]
      ; We can't really test this since the expected divider location (in pixels)
      ; is pretty hard to predict and because of the JSplitPane visibility hack
      ; that's required, it won't actually happen until it's displayed in a frame :(
      (expect true)))
  (it "should set the divider location to a percentage location with a rational (eventually)"
    (let [s (splitter :top-bottom "top" "bottom" :divider-location 1/2)]
      ; We can't really test this since the expected divider location (in pixels)
      ; is pretty hard to predict and because of the JSplitPane visibility hack
      ; that's required, it won't actually happen until it's displayed in a frame :(
      (expect true)))
  (it "should set the :divider-side"
    (= 93 (.getDividerSize (splitter :left-right (label) (label) :divider-size 93))))
  (it "should set the :resize-weight"
    (= 0.75 (.getResizeWeight (splitter :left-right (label) (label) :resize-weight 0.75))))
  (it "should set :one-touch-expandable?"
    (.isOneTouchExpandable (splitter :left-right (label) (label) :one-touch-expandable? true))))

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
      (expect (= javax.swing.JPopupMenu$Separator (class ib)))
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
          m (popup :items [a b c d])
          ; Separator isn't included in sub elements :(
          [ia ic id]  (map #(.getComponent %) (.getSubElements m))]
      (expect (= a (.getAction ia)))
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
    (= :my-frame (id-of (frame :id :my-frame))))
  (it "should create a JFrame and set its title, width, and height"
    (let [f (frame :title "Hello" :width 99 :height 88)]
      (expect (= javax.swing.JFrame (class f)))
      (expect (= "Hello" (.getTitle f)))
      (expect (= 99 (.getWidth f)))
      (expect (= 88 (.getHeight f)))))
  (it "should set the frame's size with the :size option"
    (let [f (frame :title "Hello" :size [123 :by 456])]
      (expect (= javax.swing.JFrame (class f)))
      (expect (= "Hello" (.getTitle f)))
      (expect (= 123 (.getWidth f)))
      (expect (= 456 (.getHeight f)))))
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
      (expect (= c (.getContentPane f)))))
  (it "should, by default, set location by platform to true"
    (.isLocationByPlatform (frame))))

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
       (= :my-dialog (id-of (custom-dialog :id :my-dialog))))
     (it "should create a JDialog and set its title, width, and height"
       (let [f (custom-dialog :title "Hello" :width 99 :height 88)]
         (expect (= javax.swing.JDialog (class f)))
         (expect (= "Hello" (.getTitle f)))))
     (it "should set the dialog's default close operation"
       (let [f (custom-dialog :on-close :dispose)]
         (= javax.swing.JDialog/DISPOSE_ON_CLOSE (.getDefaultCloseOperation f))))
     (it "should create a JDialog and make is not resizable"
       (let [f (custom-dialog :title "Hello" :resizable? false)]
         (expect (not (.isResizable f)))))
     (it "should create a JDialog that is modal"
       (let [f (custom-dialog :title "Hello" :modal? true)]
         (expect (.isModal f))))
     (it "should create a JDialog that is not modal"
       (let [f (custom-dialog :title "Hello" :modal? false)]
         (expect (not (.isModal f)))))
     (it "should create a JDialog and set its menu bar"
       (let [mb (menubar)
             f (custom-dialog :menubar mb)]
         (expect (= mb (.getJMenuBar f)))))
     (it "should create a JDialog and set its content pane"
       (let [c (label :text "HI")
             f (custom-dialog :content c)]
         (expect (= c (.getContentPane f))))))
    (testing "blocking"
      (it "should block until dialog is being disposed of"
        (let [dlg (custom-dialog :content "Nothing" :modal? true)]
          (expect (= (test-dlg-blocking dlg) 'dialog-is-blocking))))
      (it "should not block if :modal? is false"
        (let [dlg (custom-dialog :content "Nothing" :modal? false)]
          (expect (= (test-dlg-blocking dlg) dlg))))
      (it "should return value passed to RETURN-FROM-DIALOG"
        (let [dlg (custom-dialog :content "Nothing" :modal? true)]
          (expect (= (test-dlg-blocking
                      dlg :future-fn #(do
                                        (Thread/sleep 90)
                                        (return-from-dialog dlg :ok)
                                        (Thread/sleep 50))) :ok))))))

  
  (describe dialog
    (it "should block until dialog is being disposed of"
      (let [dlg (dialog :content "Nothing" :modal? true)]
        (expect (= (test-dlg-blocking dlg) 'dialog-is-blocking))))
    (it "should not block"
      (let [dlg (dialog :content "Nothing" :modal? false)]
        (expect (= (test-dlg-blocking dlg) dlg))))
    (testing "return-from-dialog"
      (let [ok (make-widget (action :name "Ok" :handler (fn [e] (return-from-dialog e :ok))))
            cancel (make-widget (action :name "Cancel" :handler (fn [e] (return-from-dialog e :cancel))))
            dlg (dialog :content "Nothing"
                             :options (map make-widget [ok cancel]))]
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
  (it "should create a slider with a min, max, and value"
    (let [s (slider :min 40 :max 99 :value 55)]
      (expect (= javax.swing.JSlider (class s)))
      (expect (= 40 (.getMinimum s)))
      (expect (= 99 (.getMaximum s)))
      (expect (= 55 (.getValue s)))))
          
  (verify-config (slider :snap-to-ticks? true) :snap-to-ticks? true)
  (verify-config (slider :snap-to-ticks? false) :snap-to-ticks? false)
  (verify-config (slider :paint-ticks? true) :paint-ticks? true)
  (verify-config (slider :paint-ticks? false) :paint-ticks? false)
  (verify-config (slider :paint-track? true) :paint-track? true)
  (verify-config (slider :paint-track? false) :paint-track? false)
  (verify-config (slider :inverted? true) :inverted? true)
  (verify-config (slider :inverted? false) :inverted? false)
          )

(describe progress-bar
  (it "should create a JProgressBar"
    (expect (= javax.swing.JProgressBar (class (progress-bar)))))
  (it "should set the progress bars min, max and initial value"
    (let [pb (progress-bar :value 5 :min 1 :max 6)]
      (expect (= 5 (.getValue pb)))
      (expect (= 1 (.getMinimum pb))) 
      (expect (= 6 (.getMaximum pb))))))

(describe select
  (it "should throw an exception if selector is not a vector"
    (try (do (select nil 99) false) (catch IllegalArgumentException e true)))

  (testing "when performing an #id query"
    (it "should return a single widget"
      (let [f (frame :id :my-frame)]
        (expect (= f (select f [:#my-frame])))))

    (it "should return a single widget"
      (let [c (label :id "hi")
            p (flow-panel :id :panel :items [c])
            f (frame :title "select by id" :content p)]
        (expect (= c (select f [:#hi])))
        (expect (= p (select f ["#panel"])))))))

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
        (expect (= BorderLayout/SOUTH (-> p .getLayout (.getConstraints l2))))))))

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
      (expect (= :hi (id-of result)))))

  (it "can handle a form with nested widget creation functions"
    (let [p (javax.swing.JPanel.)
          result (with-widget p (flow-panel :id "hi" :items [(label :text "Nested")]))]
      (expect (= p result))
      (expect (instance? javax.swing.JLabel (first (children p))))
      (expect (= :hi (id-of result)))))

  (it "uses a class literal as a factory and applies a constructor function to it"
    (let [result (with-widget javax.swing.JPasswordField (text :id "hi"))]
      (expect (instance? javax.swing.JPasswordField result))
      (expect (= :hi (id-of result)))))
  (it "applies a constructor function to an existing widget instance"
    (let [existing (JPanel.)
          result (with-widget existing (border-panel :id "hi"))]
      (expect (= existing result))
      (expect (= :hi (id-of existing))))))

(describe dispose!
  (it "should dispose of a JFrame"
    (let [f (pack! (frame :title "dispose!"))]
      (expect (.isDisplayable f))
      (let [result (dispose! f)]
        (expect (= result f))
        (expect (not (.isDisplayable f))))))
  (it "should dispose of a JDialog"
    (let [f (pack! (dialog :title "dispose!"))]
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

(defmacro test-paintable [func expected-class]
  `(it ~(str "creates a paintable " expected-class " for (paintable " func " :paint nil)")
      (let [p# (paintable ~func :paint nil :id :test)]
        (expect (instance? ~expected-class p#))
        (expect (= :test (id-of p#)))
        (expect (= p# (config! p# :paint (fn [~'g ~'c] nil)))))))

(describe paintable
  ; exercise paintable on all the widget types
  (test-paintable flow-panel   javax.swing.JPanel)
  (test-paintable label        javax.swing.JLabel)
  (test-paintable button       javax.swing.JButton)
  (test-paintable toggle       javax.swing.JToggleButton)
  (test-paintable checkbox     javax.swing.JCheckBox)
  (test-paintable radio        javax.swing.JRadioButton)
  (test-paintable text         javax.swing.JTextField)
  (test-paintable password     javax.swing.JPasswordField)
  (test-paintable editor-pane  javax.swing.JEditorPane)
  (test-paintable listbox      javax.swing.JList)
  (test-paintable table        javax.swing.JTable)
  (test-paintable tree         javax.swing.JTree)
  (test-paintable combobox     javax.swing.JComboBox)
  (test-paintable separator    javax.swing.JSeparator)
  (test-paintable menu         javax.swing.JMenu)
  (test-paintable popup        javax.swing.JPopupMenu)
  (test-paintable menubar      javax.swing.JMenuBar)
  (test-paintable toolbar      javax.swing.JToolBar)
  (test-paintable tabbed-panel javax.swing.JTabbedPane)
  (test-paintable slider       javax.swing.JSlider)
  (test-paintable progress-bar javax.swing.JProgressBar)

  (it "creates a paintable subclass given a class name"
    (let [lbl (paintable javax.swing.JLabel :paint nil :id :foo)]
      (expect (instance? javax.swing.JLabel lbl))
      (expect (= :foo (id-of lbl)))))

  (it "creates a label subclass given the label function and args."
    (let [lbl (paintable label :paint nil :id :foo)]
      (expect (instance? javax.swing.JLabel lbl))
      (expect (= :foo (id-of lbl)))))

  (it "creates a button subclass"
    (instance? javax.swing.JButton (paintable button :paint nil))))

(describe width
  (it "returns the width of a widget"
    (= 100 (width (xyz-panel :bounds [0 0 100 101])))))

(describe height
  (it "returns the height of a widget"
    (= 101 (height (xyz-panel :bounds [0 0 100 101])))))

(describe card-panel
  (it "creates a panel with a CardLayout"
    (let [p (card-panel :hgap 4 :vgap 3 :items [["Label" :first] [(button) :second]])]
      (expect (instance? javax.swing.JPanel p))
      (expect (instance? java.awt.CardLayout (.getLayout p)))
      (expect (= 4 (.. p getLayout getHgap)))
      (expect (= 3 (.. p getLayout getVgap)))
      (expect (= 2 (count (.getComponents p)))))))

(describe show-card!
  (it "sets the visible card in a card panel"
    (let [a (label)
          b (button)
          c (checkbox)
          p (card-panel :items [[a :first] [b :second] [c "third"]])]
      (show-card! p :second)
      (expect (visible? b))
      (show-card! p "third")
      (expect (visible? c)))))

