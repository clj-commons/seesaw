;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.border
  (:use seesaw.border)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [javax.swing.border EmptyBorder LineBorder MatteBorder TitledBorder]
           [java.awt Insets Color]))

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
  (it "creates a border with desired color (using to-color) and thickness"
    (let [b (line-border :thickness 12 :color "#FFFF00")]
      (expect (= LineBorder (class b)))
      (expect (= 12 (.getThickness b)))
      (expect (= Color/YELLOW (.getLineColor b)))))
  (it "creates a matte border with specified sides and color"
    (let [b (line-border :top 2 :left 3 :bottom 4 :right 5 :color "#FFFF00")]
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
  (it "returns nil given nil"
    (nil? (to-border nil)))
  (it "returns input if it's already a border"
    (let [b (line-border)]
      (expect (= b (to-border b)))))
  (it "creates an empty border with specified thickness for a number"
    (let [b (to-border 11)]
      (expect (= EmptyBorder (class b)))
      (expect (= (Insets. 11 11 11 11) (.getBorderInsets b)))))
  (it "returns a titled border using a resource bundle if given an i18n keyword"
    (let [b (to-border ::titled-border-test)]
      (expect (= TitledBorder (class b)))
      (expect (= "Test value from border.properties" (.getTitle b)))))
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

(describe custom-border
  (it "creates a custom border implementation"
    (instance? javax.swing.border.Border (custom-border)))
  (it "returns integer insets"
    (let [b (custom-border :insets 3)]
      (expect (= (Insets. 3 3 3 3) (.getBorderInsets b nil)))))
  (it "returns static vector insets"
    (let [b (custom-border :insets [1 2 3 4])]
      (expect (= (Insets. 1 2 3 4) (.getBorderInsets b nil)))))
  (it "calls a insets function"
    (let [b (custom-border :insets (constantly [1 2 3 4]))]
      (expect (= (Insets. 1 2 3 4) (.getBorderInsets b nil)))))
  (it "returns constant opaque? value"
    (let [b (custom-border :opaque? true)]
      (expect (.isBorderOpaque b))))
  (it "returns function opaque? value"
    (let [b (custom-border :opaque? (constantly true))]
      (expect (.isBorderOpaque b))))
  (it "calls provided paint function"
    (let [called (atom false)
          b (custom-border :paint (fn [c g x y w h] (reset! called true)))]
      (.paintBorder b nil nil 0 0 0 0)
      (expect @called))))

