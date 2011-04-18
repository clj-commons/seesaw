;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.selection
  (:use seesaw.selection)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe selection
  (testing "when given an AbstractButton (e.g. toggle or checkbox)"
    (it "returns nil when the button is not selected"
      (nil? (selection (javax.swing.JCheckBox. "something" false))))
    (it "returns a single-element seq with the text of the button if it's selected"
      (= ["something"] (selection (javax.swing.JCheckBox. "something" true)))))

  (testing "when given an AbstractButton (e.g. toggle or checkbox) and an argument"
    (it "deselects the button if the argument is nil"
      (let [cb (javax.swing.JCheckBox. "something" true)]
        (do
          (expect (= cb (selection cb nil)))
          (expect (nil? (selection cb))))))
    (it "selects the button if the argument is truthy"
      (let [cb (javax.swing.JCheckBox. "something" false)]
        (do
          (expect (= cb (selection cb "true")))
          (expect (selection cb))))))

  (testing "when given a ComboBox"
    (it "returns nil when nothing is selected"
      (nil? (selection (javax.swing.JComboBox.))))
    (it "returns a single-element seq with the selected valuej"
      (= [1] (selection (javax.swing.JComboBox. (to-array [1 2 3 4]))))))

  (testing "when given a ComboBox and an argument"
    (it "sets the selection to that argument"
      (let [cb (javax.swing.JComboBox. (to-array [1 2 3 4]))]
        (do
          (expect (= cb (selection cb 3)))
          (expect (= [3] (selection cb)))))))

  (testing "when given a JList"
    (it "returns nil when the selection is empty"
      (nil? (selection (javax.swing.JList.))))
    (it "returns the selection when it isn't empty"
      (let [jlist (javax.swing.JList. (to-array [1 2 3 4 5 6 7]))]
        (.setSelectionInterval jlist 1 3)
        (expect (= [2 3 4] (selection jlist))))))

  (testing "when given a JList and an argument"
    (it "Clears the selection when the argument is nil"
      (let [jlist (javax.swing.JList. (to-array [1 2 3 4 5 6 7]))]
        (.setSelectionInterval jlist 1 3)
        (expect (= jlist (selection jlist nil)))
        (expect (nil? (selection jlist)))))
    (it "Selects the given *values* when argument is a non-empty seq"
      (let [jlist (javax.swing.JList. (to-array [1 "test" 3 4 5 6 7]))]
        (expect (= jlist (selection jlist ["test" 4 6])))
        (expect (= ["test" 4 6] (selection jlist)))))))

