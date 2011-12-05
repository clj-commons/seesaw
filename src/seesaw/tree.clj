;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.tree)

(defprotocol TreeModelEventSource
  (fire-event*
    [this event-type event]
    "Dispatches a TreeModelEvent to all model listeners. event-type is one of
     :tree-nodes-changed, :tree-nodes-inserted, :tree-nodes-removed or
     :tree-structure-changed. Note, do not use this function directly.
    Instead use one of the helper functions in (seesaw.tree)."))

(defn- ensure-array
  [v] (if v (to-array v)))

(defn node-structure-changed 
  "Fire a node structure changed event on a tree model created with 
  (simple-tree-model). node-path is the sequence of nodes from the model
  root to the node whose structure changed.

  Call this when the entire structure under a node has changed.
  
  See:
    (seesaw.tree/simple-tree-model)
  "
  [tree-model node-path]
  (fire-event* tree-model
              :tree-structure-changed
              (javax.swing.event.TreeModelEvent. tree-model
                                                 (ensure-array node-path)
                                                 nil
                                                 nil)))

(defn nodes-removed 
  "Fire a node removed event on a tree model created with 
  (simple-tree-model). parent-path is the path to the parent node,
  indices is a seq of the indices of the removed nodes and children
  is a seq of the removed nodes.

  See:
    (seesaw.tree/simple-tree-model)
    (seesaw.tree/node-removed)
  "
  [tree-model parent-path indices children]
  (fire-event* tree-model
              :tree-nodes-removed
              (javax.swing.event.TreeModelEvent. tree-model 
                                                 (ensure-array parent-path)
                                                 (int-array indices)
                                                 (ensure-array children))))

(defn node-removed 
  "Fire a node removed event on a tree model created with 
  (simple-tree-model). parent-path is the path to the parent node,
  index is the index of the removed node and child is the removed node.

  See:
    (seesaw.tree/nodes-removed)
    (seesaw.tree/simple-tree-model)
  "
  [tree-model parent-path index child]
  (nodes-removed tree-model parent-path [index] [child]))

(defn- build-insert-or-change-event [tree-model parent-path children]
  (let [indices (if-let [parent (last parent-path)] 
                  (map #(.getIndexOfChild tree-model parent %) children)) ]
    (javax.swing.event.TreeModelEvent. tree-model 
                                       (ensure-array parent-path)
                                       (if indices (int-array indices))
                                       (ensure-array children))))

(defn nodes-inserted 
  "Fire a node insertion event. parent-path is the path to the parent of the
  newly inserted children. children is the newly inserted nodes.
  
  See:
    (seesaw.tree/node-inserted)
    (seesaw.tree/simple-tree-model)
  "
  [tree-model parent-path children]
  (fire-event* tree-model 
              :tree-nodes-inserted
              (build-insert-or-change-event tree-model parent-path children)))

(defn node-inserted 
  "Fire a node insertion event. parent-path is the path to the parent of the
  newly inserted child. child is the newly inserted node.
  
  See:
    (seesaw.tree/nodes-inserted)
    (seesaw.tree/simple-tree-model)
  "
  [tree-model node-path]
  (let [parent-path (butlast node-path)
        node        (last node-path)]
    (nodes-inserted tree-model 
                   (or parent-path [node]) 
                   (if parent-path [node]))))

(defn nodes-changed 
  "Fire a node changed event. parent-path is the path to the parent of the
  changed children. children is the changed nodes.

  Fire this event if the appearance of a node has changed in any way.
  
  See:
    (seesaw.tree/node-changed)
    (seesaw.tree/simple-tree-model)
  "
  [tree-model parent-path children]
  (fire-event* tree-model 
              :tree-nodes-changed 
              (build-insert-or-change-event tree-model parent-path children)))

(defn node-changed 
  "Fire a node changed event. parent-path is the path to the parent of the
  changed node. child is the changed node.

  Fire this event if the appearance of a node has changed in any way.
  
  See:
    (seesaw.tree/nodes-changed)
    (seesaw.tree/simple-tree-model)
  "
  [tree-model node-path]
  (let [parent-path (butlast node-path)
        node        (last node-path)]
    (nodes-changed tree-model 
                   (or parent-path [node]) 
                   (if parent-path [node]))))

(defn simple-tree-model
  "Create a simple, read-only TreeModel for use with seesaw.core/tree.
   The arguments are the same as clojure.core/tree-seq. Changes to the
  underlying model can be reported with the various node-xxx event
  functions in seesaw.tree.
  
  See:
    http://docs.oracle.com/javase/6/docs/api/javax/swing/tree/TreeModel.html
  "
  [branch? children root]
  (let [listeners (atom [])]
    (reify
      javax.swing.tree.TreeModel
      (getRoot [this] root)
      (getChildCount [this parent] (count (children parent)))
      (getChild [this parent index] (nth (children parent) index))
      (getIndexOfChild [this parent child] 
        (first (keep-indexed #(when (= %2 child) %1) (children parent))))
      (isLeaf [this node] (not (branch? node)))
      (addTreeModelListener [this listener]
        (swap! listeners conj listener))
      (removeTreeModelListener [this listener]
        (swap! listeners #(remove (partial = listener) %)))
      (valueForPathChanged [this path newValue])

      TreeModelEventSource
      (fire-event* [this event-type event]
        (let [handler (condp = event-type
                        :tree-nodes-changed     #(.treeNodesChanged %1 %2)
                        :tree-nodes-inserted    #(.treeNodesInserted %1 %2)
                        :tree-nodes-removed     #(.treeNodesRemoved %1 %2)
                        :tree-structure-changed #(.treeStructureChanged %1 %2))]
          (doseq [listener @listeners]
            (handler listener event)))))))

