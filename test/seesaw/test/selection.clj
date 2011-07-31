;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.selection
  (:require [seesaw.core :as sc])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]
        seesaw.selection
        seesaw.action))

(describe selection
  (testing "when given an Action"
      (it "returns nil when the action is not selected"
        (nil? (selection (action) )))
      (it "returns a single-element seq with true if the action is selected and multi? is given"
        (= [true] (selection (action :selected? true) {:multi? true})))
      (it "returns a single-element seq with true if the action is selected"
        (= true (selection (action :selected? true)))))
  (testing "when given an AbstractButton (e.g. toggle or checkbox)"
    (it "returns nil when the button is not selected"
      (nil? (selection (javax.swing.JCheckBox. "something" false))))
    (it "returns the button itself if it is selected"
      (let [b (javax.swing.JCheckBox. "something" true)]
        (expect (= b (selection b)))))
    (it "returns a single-element seq with the button itself if it's selected and multi? is true"
      (let [b (javax.swing.JCheckBox. "something" true)] 
        (expect (= [b] (selection b {:multi? true}))))))

  (testing "when given a ButtonGroup"
    (it "returns nil when no button is selected"
      (nil? (selection (sc/button-group :buttons [(sc/toggle) (sc/radio)]))))
    (it "returns the first selected button in the group"
      (let [b (sc/toggle :selected? true)]
        (expect (= b (selection (sc/button-group :buttons [(sc/toggle) b (sc/radio)])))))))

  (testing "when given a ComboBox"
    (it "returns nil when nothing is selected"
      (nil? (selection (javax.swing.JComboBox.))))
    (it "returns a single-element seq with the selected value when multi? is true"
      (= [1] (selection (javax.swing.JComboBox. (to-array [1 2 3 4])) {:multi? true}))))

  (testing "when given a JTree"
    (it "returns nil when the selection is empty"
      (nil? (selection (javax.swing.JTree.))))
    (it "returns the selection as a seq of paths when it isn't empty"
      (let [jtree (javax.swing.JTree. (to-array [1 2 3 4 5]))]
        (.setSelectionInterval jtree 1 3)
        ; Note. This kind of sucks because the JTree constructor used above
        ; creates a tree of JTree.DynamicUtilTreeNode rather than just ints.
        ; If a real TreeModel was used, it could be more reasonable.
        (expect (= [["root" 2] ["root" 3] ["root" 4]] 
                  (map (fn [path] (map #(.getUserObject %) path)) (selection jtree {:multi? true})))))))

  (testing "when given a JList"
    (it "returns nil when the selection is empty"
      (nil? (selection (javax.swing.JList.))))
    (it "returns the selection when it isn't empty"
      (let [jlist (javax.swing.JList. (to-array [1 2 3 4 5 6 7]))]
        (.setSelectionInterval jlist 1 3)
        (expect (= 2 (selection jlist)))
        (expect (= [2 3 4] (selection jlist {:multi? true}))))))

  (testing "when given a JSlider"
    (it "returns the current value"
      (= 32 (selection (sc/slider :min 0 :max 100 :value 32)))))

  (testing "when given a JTextComponent"
    (it "returns nil when the selection is empty"
      (nil? (selection (javax.swing.JTextField. "HELLO"))))
    (it "returns a range vector [start end] when the selection is non-empty"
      (let [t (javax.swing.JTextField. "HELLO")]
        (.select t 2 4)
        (expect (= [2 4] (selection t))))))

  (testing "when given a JTable"
    (it "returns nil when no rows are selected"
      (nil? (selection (javax.swing.JTable.))))
    (it "returns a seq of selected model row indices when selection is non-empty"
      (let [jtable (javax.swing.JTable. 5 3)]
        (.setRowSelectionInterval jtable 1 3)
        (= [1 2 3] (selection jtable {:multi? true}))
        (= 1 (selection jtable))))))
          

(describe selection!
  (testing "when given an AbstractButton (e.g. toggle or checkbox) and an argument"
    (it "deselects the button if the argument is nil"
      (let [cb (javax.swing.JCheckBox. "something" true)]
        (do
          (expect (= cb (selection! cb nil)))
          (expect (nil? (selection cb))))))
    (it "selects the button if the argument is truthy"
      (let [cb (javax.swing.JCheckBox. "something" false)]
        (do
          (expect (= cb (selection! cb "true")))
          (expect (selection cb))))))

  (testing "when given a ButtonGroup and an argument"
    (it "deselects the button if the argument is nil"
      (let [bg (sc/button-group :buttons [(sc/toggle) (sc/radio :selected? true) (sc/radio)])]
        (do
          (expect (= bg (selection! bg nil)))
          (expect (nil? (selection bg))))))
    (it "selects a button if the argument is a button"
      (let [b (sc/radio)
            bg (sc/button-group :buttons [(sc/toggle :selected? true) b (sc/radio)])]
        (do
          (expect (= bg (selection! bg b)))
          (expect (= b (selection bg)))
          (expect (.isSelected b))))))

  (testing "when given a ComboBox and an argument"
    (it "sets the selection to that argument"
      (let [cb (javax.swing.JComboBox. (to-array [1 2 3 4]))]
        (do
          (expect (= cb (selection! cb 3)))
          (expect (= 3 (selection cb)))))))

  (testing "when given a JSlider and an argument"
    (it "sets the slider value to that argument"
      (let [s (sc/slider :min 0 :max 100 :value 0)
            result (selection! s 32)]
        (expect (= result s))
        (expect (= 32 (.getValue s))))))

  (testing "when given a JTree and an argument"
    (it "Clears the selection when the argument is nil"
      (let [jtree (javax.swing.JTree. (to-array [1 2 3 4 5]))]
        (.setSelectionInterval jtree 1 3)
        (expect (= jtree (selection! jtree nil)))
        (expect (nil? (selection jtree))))))

  (testing "when given a JList and an argument"
    (it "Clears the selection when the argument is nil"
      (let [jlist (javax.swing.JList. (to-array [1 2 3 4 5 6 7]))]
        (.setSelectionInterval jlist 1 3)
        (expect (= jlist (selection! jlist nil)))
        (expect (nil? (selection jlist)))))
    (it "Selects the given *values* when argument is a non-empty seq"
      (let [jlist (javax.swing.JList. (to-array [1 "test" 3 4 5 6 7]))]
        (expect (= jlist (selection! jlist {:multi? true} ["test" 4 6])))
        (expect (= ["test" 4 6] (selection jlist {:multi? true})))
        (expect (= "test" (selection jlist))))))

  (testing "when given a text component"
    (it "Clears the selection when the argument is nil"
      (let [t (javax.swing.JTextArea. "This is some text with a selection")]
        (.select t 5 10)
        (selection! t nil)
        (expect (nil? (selection t)))))
    (it "sets the selection given a [start end] range vector"
      (let [t (javax.swing.JTextArea. "THis is more text with a selection")]
        (selection! t [4 9])
        (expect (= [4 9] (selection t))))))

  (testing "when given a JTable and an argument"
    (it "Clears the row selection when the argument is nil"
      (let [jtable (javax.swing.JTable. 5 3)]
        (.setRowSelectionInterval jtable 1 3)
        (expect (= jtable (selection! jtable nil)))
        (expect (nil? (selection jtable)))))
    (it "selects the given rows when argument is a non-empty seq of row indices"
      (let [jtable (javax.swing.JTable. 10 2)]
        (expect (= jtable (selection! jtable {:multi? true } [0 2 4 6 8 9])))
        (expect (= [0 2 4 6 8 9] (selection jtable {:multi? true})))
        (expect (= 0 (selection jtable)))))))

