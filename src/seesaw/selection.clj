;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.selection
  (:use seesaw.util))

(defprotocol Selection
  (get-selection [target])
  (set-selection [target args]))

(extend-protocol Selection
  javax.swing.Action
    (get-selection [target]      
      (when-let [s (.getValue target javax.swing.Action/SELECTED_KEY)] [true]))
    (set-selection [target [v]] (.putValue target javax.swing.Action/SELECTED_KEY (boolean v)))

  javax.swing.AbstractButton
    (get-selection [target]      (if (.isSelected target) [target]))
    (set-selection [target [v]]  (doto target (.setSelected (boolean v))))

  javax.swing.ButtonGroup
    (get-selection [^javax.swing.ButtonGroup target]      
      (when-let [sel (some #(when (.isSelected ^javax.swing.AbstractButton %) %) (enumeration-seq (.getElements target)))]
        [sel]))
    (set-selection [^javax.swing.ButtonGroup target [^javax.swing.AbstractButton v]]  
      (if v
        (.setSelected target (.getModel v) true)
        (.clearSelection target))
      target)

  javax.swing.JSlider
    (get-selection [target]     (vector (.getValue target)))
    (set-selection [target [v]] (doto target (.setValue v)))

  javax.swing.JComboBox
    (get-selection [target]     (seq (.getSelectedObjects target)))
    (set-selection [target [v]] (doto target (.setSelectedItem v))))

(defn- list-model-to-seq
  [^javax.swing.ListModel model]
  (map #(.getElementAt model %) (range 0 (.getSize model))))

(defn- list-model-indices
  [model values]
  (let [value-set    (if (set? values) values (apply hash-set values))]
    (->> (list-model-to-seq model)
      (map-indexed #(vector %1 (value-set %2)))
      (filter second)
      (map first))))

(defn- jlist-set-selection
  ([^javax.swing.JList target values]
    (if (seq values)
      (let [indices (list-model-indices (.getModel target) values)]
        (.setSelectedIndices target (int-array indices)))
      (.clearSelection target))
   target))

(extend-protocol Selection
  javax.swing.JList
    (get-selection [target]      (seq (.getSelectedValues target)))
    (set-selection [target args] (jlist-set-selection target args)))

(extend-protocol Selection
  javax.swing.JTable
    (get-selection [target] (seq (map #(.convertRowIndexToModel target %) (.getSelectedRows target))))
    (set-selection [target args] 
      (if (seq args)
        (do 
          (.clearSelection target)
          (doseq [i args] (.addRowSelectionInterval target i i)))
        (.clearSelection target))))

(extend-protocol Selection
  javax.swing.JTree
    (get-selection [target] (seq (map #(seq (.getPath ^javax.swing.tree.TreePath %)) (.getSelectionPaths target))))
    (set-selection [target args]
      (if (seq args)
        target
        (.clearSelection target))))

(extend-protocol Selection
  javax.swing.text.JTextComponent
  (get-selection [target]
    (let [start (.getSelectionStart target)
          end   (.getSelectionEnd target)]
      (if-not (= start end) [[start end]])))
  (set-selection [target [args]]
    (if (integer? args)
      (.select target args args)
    (if-let [[start end] args]
      (.select target start end) 
      (.select target 0 0)))))

(defn selection
  ([target] (selection target {}))
  ([target opts] 
    (let [s (get-selection target)]
      (if (:multi? opts) 
        s 
        (first s)))))

(defn selection!
  ([target values] (selection! target {} values))
  ([target opts values] 
    (check-args (not (nil? target)) "target of selection! cannot be nil")
    (set-selection 
      target 
      (if (or (nil? values) (:multi? opts)) values [values]) )
    target))



