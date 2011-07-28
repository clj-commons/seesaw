;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.tree)

(defn simple-tree-model
  "Create a simple, read-only TreeModel for use with seesaw.core/tree.
   The arguments are the same as clojure.core/tree-seq."
  [branch? children root]
  (reify javax.swing.tree.TreeModel
    (getRoot [this] root)
    (getChildCount [this parent] (count (children parent)))
    (getChild [this parent index] (nth (children parent) index))
    (getIndexOfChild [this parent child] 
      (first (keep-indexed #(when (= %2 child) %1) (children parent))))
    (isLeaf [this node] (not (branch? node)))
    (addTreeModelListener [this listener])
    (removeTreeModelListener [this listener])
    (valueForPathChanged [this path newValue])))

(defn update-tree! 
  "Update a tree.
  The model is optional, if not supplied this function refreshes 
  the tree (useful for e.g. file trees).
  
  Expanded nodes will still be expanded after update, given that
  the expanded node didn't change.
  "
  {:arglists '([tree model?])}
  [tree & [model]]
  (if model
    (let [visible_paths (doall
                          (for [row (range (.getRowCount tree))]
                            (.getPathForRow tree row)))]
      (.setModel tree (if model model (.getModel tree)))
      (doseq [path visible_paths]
        (.makeVisible tree path)))
    (.updateUI tree))
  tree)
