;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.scroll
  (:use seesaw.scroll
        seesaw.core)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(defn make-test-table [] (table :model [:columns [:a :b] :rows [[0 1] [2 3] [3 4] [4 5]]]))

; Most of these don't test anything, only exercise the code
(describe scroll!*
  (testing "given an arbitrary component"
    (it "can scroll to :top"
      (let [p (canvas)
            s (scrollable p)]
        (scroll!* p :to :top)))
    (it "can scroll to :bottom"
      (let [p (canvas)
            s (scrollable p)]
        (scroll!* p :to :bottom)))
    (it "can scroll to a java.awt.Point"
      (let [p (canvas)
            s (scrollable p)]
        (scroll!* p :to (java.awt.Point. 20 20))))
    (it "can scroll to a java.awt.Rectangle"
      (let [p (canvas)
            s (scrollable p)]
        (scroll!* p :to (java.awt.Rectangle. 20 20 10 10))))
    (it "can scroll to [:point x y]"
      (let [p (canvas)
            s (scrollable p)]
        (scroll!* p :to [:point 10 10])))
    (it "can scroll to [:rect x y w h]"
      (let [p (canvas)
            s (scrollable p)]
        (scroll!* p :to [:rect 10 10 20 20]))))

  (testing "given a listbox (JList)"
    (it "can scroll to [:row n]"
      (let [lb (listbox :model [1 2 3 4 5])
            s (scrollable lb)]
        (scroll!* lb :to [:row 3]))))

  (testing "given a table (JTable)"
    (it "can scroll to [:row n]"
      (let [t (make-test-table)
            s (scrollable t)]
        (scroll!* t :to [:row 2])))
    (it "can scroll to [:column n]"
      (let [t (make-test-table)
            s (scrollable t)]
        (scroll!* t :to [:column 1])))
    (it "can scroll to [:cell row col]"
      (let [t (make-test-table)
            s (scrollable t)]
        (scroll!* t :to [:cell 3 1]))))

  (testing "given a test component"
    (it "can scroll to [:line n]"
      (let [t (text :multi-line? true :text "\n\n\n\n")
            s (scrollable t)]
        (scroll!* t :to [:line 2])
        (expect (= 2 (.getCaretPosition t)))))
    (it "can scroll to [:position n]"
      (let [t (text :multi-line? true :text "\n\n\n\n")
            s (scrollable t)]
        (scroll!* t :to [:position 4])
        (expect (= 4 (.getCaretPosition t)))))))



