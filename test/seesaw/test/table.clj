;  Copyright (c) Dave Ray, 2011. All ritest/seesaw/test/core.clj

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.table
  (:use seesaw.table)
  (:use [lazytest.describe :only (describe it testing given)]
        [lazytest.expect :only (expect)]))

(describe table-model
  (it "should create a table model"
    (instance? javax.swing.table.TableModel (table-model)))

  (it "should create columns from a list of keys"
    (let [t (table-model :columns [:key1 :key2])]
      (expect (= "key1" (.getColumnName t 0)))
      (expect (= "key2" (.getColumnName t 1)))))

  (it "should create columns from a list of maps and keys"
    (let [t (table-model :columns [{:key :key1 :text "KEY1"} :key2])]
      (expect (= "KEY1" (.getColumnName t 0)))
      (expect (= "key2" (.getColumnName t 1)))))

  (it "should create rows from a list of maps"
    (let [t (table-model :columns [:a :b] :rows [{:a "a0" :b "b0"} {:a "a1" :b "b1"}])]
      (expect (= 2 (.getRowCount t)))
      (expect (= "a0" (.getValueAt t 0 0)))
      (expect (= "b0" (.getValueAt t 0 1)))
      (expect (= "a1" (.getValueAt t 1 0)))
      (expect (= "b1" (.getValueAt t 1 1)))))
          
  (it "should create rows from a list of vectors"
    (let [t (table-model :columns [:a :b] :rows [["a0" "b0"] ["a1" "b1"]])]
      (expect (= 2 (.getRowCount t)))
      (expect (= "a0" (.getValueAt t 0 0)))
      (expect (= "b0" (.getValueAt t 0 1)))
      (expect (= "a1" (.getValueAt t 1 0)))
      (expect (= "b1" (.getValueAt t 1 1)))))
  
  (it "makes column metadata available through (.getValueAt model -1 -1)"
    (let [t (table-model :columns [:a :b])]
      (expect (= 1 (:b (.getValueAt t -1 -1)))))))

(describe value-at
  (it "gets the value of a row as a map"
    (let [t (table-model :columns [:a :b] :rows [["a0" "b0"] ["a1" "b1"]])]
      (expect (= {:a "a0" :b "b0" } (value-at t 0)))))
  (it "gets the value of a row as a map (indexed by integers) if model was not
      created with (table-model)"
    (let [t (javax.swing.table.DefaultTableModel. 2 3)]
      (expect (= {"A" nil "B" nil "C" nil } (value-at t 0)))))
  (it "gets the value of multiple rows as a list of maps"
    (let [t (table-model :columns [:a :b] :rows [["a0" "b0"] ["a1" "b1"]])]
      (expect (= [{:a "a0" :b "b0" } {:a "a1" :b "b1" }] (value-at t [0 1]))))))


