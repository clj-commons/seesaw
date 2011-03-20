(ns seesaw.test.core
  (:use seesaw.core)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import (javax.swing JPanel JButton Box Box$Filler BoxLayout)
           (javax.swing.border EmptyBorder LineBorder MatteBorder TitledBorder)
           (java.awt Insets Color Dimension)))

(describe empty-border
  (it "creates a 1 pixel border by default"
      (let [b (empty-border)]
        (expect (= EmptyBorder (class b)))
        (expect (= (Insets. 1 1 1 1) (.getBorderInsets b)))))
  (it "creates a border with same thickness on all sides"
    (let [b (empty-border :thickness 11)]
      (expect (= EmptyBorder (class b)))
      (expect (= (Insets. 11 11 11 11) (.getBorderInsets b)))))
  (it "creates a border with specified sides"
    (let [b (empty-border :top 2 :left 3 :bottom 4 :right 5)]
      (expect (= EmptyBorder (class b)))
      (expect (= (Insets. 2 3 4 5) (.getBorderInsets b)))))
  (it "creates a border with specified sides, defaulting to 0"
    (let [b (empty-border :left 3 )]
      (expect (= EmptyBorder (class b)))
      (expect (= (Insets. 0 3 0 0) (.getBorderInsets b))))))

(describe line-border
  (it "creates a black, one pixel border by default"
    (let [b (line-border)]
      (expect (= LineBorder (class b)))
      (expect (= 1 (.getThickness b)))
      (expect (= Color/BLACK (.getLineColor b)))))
  (it "creates a border with desired color and thickness"
    (let [b (line-border :thickness 12 :color Color/YELLOW)]
      (expect (= LineBorder (class b)))
      (expect (= 12 (.getThickness b)))
      (expect (= Color/YELLOW (.getLineColor b)))))
  (it "creates a matte border with specified sides"
    (let [b (line-border :top 2 :left 3 :bottom 4 :right 5 :color Color/YELLOW)]
      (expect (= MatteBorder (class b)))
      (expect (= (Insets. 2 3 4 5) (.getBorderInsets b)))
      (expect (= Color/YELLOW (.getMatteColor b)))))
  (it "creates a matte border with specified sides, defaulting to 0"
    (let [b (line-border :top 2)]
      (expect (= MatteBorder (class b)))
      (expect (= (Insets. 2 0 0 0) (.getBorderInsets b)))
      (expect (= Color/BLACK (.getMatteColor b))))))

(describe compound-border
  (it "creates nested compound borders inner to outer"
    (let [in (line-border)
          mid (line-border)
          out (line-border)
          b (compound-border in mid out)]
      (expect (= out (.getOutsideBorder b)))
      (expect (= mid (.. b (getInsideBorder) (getOutsideBorder))))
      (expect (= in (.. b (getInsideBorder) (getInsideBorder)))))))

(describe to-border
  (it "returns input if it's already a border"
    (let [b (line-border)]
      (expect (= b (to-border b)))))
  (it "returns a titled border using str if it doesn't know what to do"
    (let [b (to-border "Test")]
      (expect (= TitledBorder (class b)))
      (expect (= "Test" (.getTitle b)))))
  (it "creates a compound border out of multiple args"
      (let [b (to-border "Inner" "Outer")]
        (expect (= "Outer" (.. b getOutsideBorder getTitle)))
        (expect (= "Inner" (.. b getInsideBorder getTitle)))))
  (it "creates a compound border out of a collection arg"
      (let [b (to-border ["Inner" "Outer"])]
        (expect (= "Outer" (.. b getOutsideBorder getTitle)))
        (expect (= "Inner" (.. b getInsideBorder getTitle))))))

(describe apply-default-opts 
  (testing "setting opaque option"
    (it "does nothing when omitted"
      (let [c (apply-default-opts (JPanel.))]
        (expect (= true (.isOpaque c)))))
    (it "sets opacity when provided"
      (let [c (apply-default-opts (JPanel.) {:opaque false})]
        (expect (= false (.isOpaque c))))))
  (it "sets background when provided"
      (let [c (apply-default-opts (JPanel.) {:background Color/BLACK})]
        (expect (= Color/BLACK (.getBackground c)))))
  (it "sets foreground when provided"
      (let [c (apply-default-opts (JPanel.) {:foreground Color/GREEN})]
        (expect (= Color/GREEN (.getForeground c)))))
  (it "sets border when provided using to-border"
      (let [c (apply-default-opts (JPanel.) {:border "TEST"})]
        (expect (= "TEST" (.. c getBorder getTitle))))))

(describe to-widget
  (it "returns input if it's already a widget"
    (let [c (JPanel.)]
      (expect (= c (to-widget c)))))
  (it "returns a label for text input"
    (let [c (to-widget "TEST")]
      (expect (= "TEST" (.getText c)))))
  (it "returns a button if input is an Action"
    (let [a (make-action #(println "HI") :name "Test")
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
      (expect (= 34 (.. c getPreferredSize getHeight)))
      )))

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

