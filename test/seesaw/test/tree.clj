;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.tree
  (:use seesaw.tree)
  (:use [lazytest.describe :only (describe do-it it testing given)]
        [lazytest.expect :only (expect)]))

(describe simple-tree-model
  (given [branch? (fn [node] (= node "dir"))
        children (fn [node] (when (= node "dir") [1 2 3]))
        m (simple-tree-model branch? children "dir")]
    (it "should create a read-only tree model from branch? and children functions"
      (instance? javax.swing.tree.TreeModel m))
    (it "should return the root"
      (= "dir" (.getRoot m)))
    (it "should return isLeaf"
      (expect (.isLeaf m "file"))
      (expect (not (.isLeaf m "dir"))))
    (it "should return the child count"
      (expect (= 0 (.getChildCount m "file")))
      (expect (= 3 (.getChildCount m "dir"))))
    (it "should return a child by index"
      (= [1 2 3] (map #(.getChild m "dir" %) (range 3))))
    (it "should retrieve the index of a child"
      (= [0 1 2] (map #(.getIndexOfChild m "dir" %) [1 2 3])))))

(describe update-tree!
  (given [tree (javax.swing.JTree. (simple-tree-model (constantly true) #(range (inc %)) 1))
          path (javax.swing.tree.TreePath. (into-array [1 1 1 1]))]
    (do-it "expand path"
      (.makeVisible tree path))
    (it "should be visible before update"
      (.isVisible tree path))
    (do-it "update tree"
      (update-tree! tree (simple-tree-model (constantly true) #(range (+ % 2)) 1)))
    (it "should update the tree"
        (= [1 2]
           (vec (.getPath (.getPathForRow tree (dec (.getRowCount tree)))))))
    (it "should retain expanded paths after update"
      (.isVisible tree path))))
