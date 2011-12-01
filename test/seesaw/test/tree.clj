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
  (:use [seesaw.core :only [listen]])
  (:use [lazytest.describe :only (describe it testing given)]
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

(describe fire-event
  (it "fires events of the correct type"
    (let [m (simple-tree-model (constantly true) (constantly nil) :x)
          nodes-changed (atom nil)
          nodes-inserted (atom nil)
          nodes-removed (atom nil)
          structure-changed (atom nil)
          ; dummy args
          event-source :foo
          path-to-node [:bar]]
      (listen m
        :tree-nodes-changed #(reset! nodes-changed %)
        :tree-nodes-inserted #(reset! nodes-inserted %)
        :tree-nodes-removed #(reset! nodes-removed %)
        :tree-structure-changed #(reset! structure-changed %))
      (expect (not (or @nodes-changed @nodes-inserted @nodes-removed @structure-changed)))
      (fire-event m :tree-nodes-changed event-source path-to-node)
      (expect @nodes-changed)
      (fire-event m :tree-nodes-inserted event-source path-to-node)
      (expect @nodes-inserted)
      (fire-event m :tree-nodes-removed event-source path-to-node)
      (expect @nodes-removed)
      (fire-event m :tree-structure-changed event-source path-to-node)
      (expect @structure-changed))))
