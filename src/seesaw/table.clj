;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.table
  (:use [seesaw.util :only [illegal-argument]]))

(defn- normalize-column [c]
  (conj {:text  (get c :text ((fnil name c) (:key c)))
         :class (get c :class Object)}
        (if (map? c)
          (select-keys c [:key :text :class])
          {:key c})))

(defn- unpack-row-map [col-key-map row]
  (let [a (object-array (inc (count col-key-map)))]
    (doseq [[k v] row]
      (if-let [col-key (get col-key-map k)]
        (aset a col-key v)))
    (aset a (count col-key-map) row)
    a))

(defn- unpack-row [col-key-map row]
  (cond
    (map? row)    (unpack-row-map col-key-map row)
    (vector? row) (object-array (concat row [nil]))
    :else         (illegal-argument "row must be a map or vector, got %s" (type row))))

(defn- insert-at [row-vec pos item]
  (apply conj (subvec row-vec 0 pos) item (subvec row-vec pos)))

(defn- remove-at [row-vec pos]
  (let [[head [_ & tail]] (split-at pos row-vec)]
    (vec (concat head tail))))

(defn- ^javax.swing.table.DefaultTableModel proxy-table-model
  [column-names column-key-map column-classes]
  (let [full-values (atom [])]
    (proxy [javax.swing.table.DefaultTableModel] [(object-array column-names) 0]
      (isCellEditable [row col] false)
      (setRowCount [^Integer rows]
        ; trick to force proxy-super macro to see correct type to avoid reflection.
        (swap! full-values (fn [v]
                             (if (< rows (count v))
                               (subvec v rows)
                               (vec (concat v (take (- (count v) rows) (constantly nil)))))))
        (let [^javax.swing.table.DefaultTableModel this this]
          (proxy-super setRowCount rows)))
      (addRow [^objects values]
        (swap! full-values conj (last values))
        ; TODO reflection - I can't get rid of the reflection here without crashes
        ; It has something to do with Object[] vs. Vector overrides.
        (proxy-super addRow values))
      (insertRow [row ^objects values]
        (swap! full-values insert-at row (last values))
        ; TODO reflection - I can't get rid of the reflection here without crashes
        ; It has something to do with Object[] vs. Vector overrides.
        (proxy-super insertRow row values))
      (removeRow [row]
        (swap! full-values remove-at row)
        (let [^javax.swing.table.DefaultTableModel this this]
          (proxy-super removeRow row)))
      ; TODO this stuff is an awful hack and now that I'm wiser, I should fix it.
      (getValueAt [row col]
        (if (= -1 row col)
          column-key-map
          (if (= -1 col)
            (get @full-values row)
            (let [^javax.swing.table.DefaultTableModel this this]
              (proxy-super getValueAt row col)))))
      (setValueAt [value row col]
        (if (= -1 col)
          (swap! full-values assoc row value)
          (let [^javax.swing.table.DefaultTableModel this this]
            (proxy-super setValueAt value row col))))
      (getColumnClass [^Integer c]
        (proxy-super getColumnClass c)
        (nth column-classes c)))))

(defn- get-full-value [^javax.swing.table.TableModel model row]
  (try
    ; Try to grab the full value using proxy hack above
    (.getValueAt model row -1)
    (catch ArrayIndexOutOfBoundsException e nil)))

(defn- get-column-key-map [^javax.swing.table.TableModel model]
  (try
    ; Try to grab the column to key map using proxy hack above
    (.getValueAt model -1 -1)
    (catch ArrayIndexOutOfBoundsException e
      ; Otherwise, just map from column names to values
      (let [n (.getColumnCount model)]
        (apply hash-map
               (interleave
                 (map #(.getColumnName model %) (range n))
                 (range n)))))))

(defn table-model
  "Creates a TableModel from column and row data. Takes two options:

    :columns - a list of keys, or maps. If a key, then (name key) is used as the 
               column name. If a map, it can be in the form 
               {:key key :text text :class class} where key is use to index the 
               row data, text (optional) is used as the column name, and 
               class (optional) specifies the object class of the column data
               returned by getColumnClass. The order establishes the order of the
               columns in the table.

    :rows - a sequence of maps or vectors, possibly mixed. If a map, must contain
            row data indexed by keys in :columns. Any additional keys will
            be remembered and retrievable with (value-at). If a vector, data
            is indexed by position in the vector.

  Example:

    (table-model :columns [:name
                           {:key :age :text \"Age\" :class java.lang.Integer}]
                 :rows [ [\"Jim\" 65]
                         {:age 75 :name \"Doris\"}])

    This creates a two column table model with columns \"name\" and \"Age\"
    and two rows.

  See:
    (seesaw.core/table)
    http://download.oracle.com/javase/6/docs/api/javax/swing/table/TableModel.html
  "
  [& {:keys [columns rows] :as opts}]
  (let [norm-cols   (map normalize-column columns)
        col-names   (map :text norm-cols)
        col-classes (map :class norm-cols)
        col-key-map (reduce (fn [m [k v]] (assoc m k v)) {} (map-indexed #(vector (:key %2) %1) norm-cols))
        model (proxy-table-model col-names col-key-map col-classes)]
    (doseq [row rows]
      (.addRow model ^objects (unpack-row col-key-map row)))
    model))

; TODO this is used in places that assume DefaultTableModel
(defn- ^javax.swing.table.DefaultTableModel to-table-model [v]
  (cond
    (instance? javax.swing.table.TableModel v) v
    ; TODO replace with (to-widget) so (value-at) works with events and stuff
    (instance? javax.swing.JTable v) (.getModel ^javax.swing.JTable v)
    :else (illegal-argument "Can't get table model from %s" v)))

(defn- single-value-at
  [^javax.swing.table.TableModel model col-key-map row]
  (if (and (>= row 0) (< row (.getRowCount model)))
    (let [full-row (get-full-value model row)]
      (merge
        full-row
        (reduce
          (fn [result k] (assoc result k (.getValueAt model row (col-key-map k))))
          {}
          (keys col-key-map))))
    nil))

(defn value-at
  "Retrieve one or more rows from a table or table model. target is a JTable or TableModel.
  rows is either a single integer row index, or a sequence of row indices. In the first case
  a single map of row values is returns. Otherwise, returns a sequence of maps.

  If a row index is out of bounds, returns nil.

  Notes:

  If target was not created with (table-model), the returned map(s) are indexed
  by column name.

  Any non-column keys passed to (update-at!) or the initial rows of (table-model)
  are *remembered* and returned in the map.

  Examples:

    ; Retrieve row 3
    (value-at t 3)

    ; Retrieve rows 1, 3, and 5
    (value-at t [1 3 5])

    ; Print values of selected rows
    (listen t :selection
      (fn [e]
        (println (value-at t (selection {:multi? true} t)))))
  See:
    (seesaw.core/table)
    (seesaw.table/table-model)
    http://download.oracle.com/javase/6/docs/api/javax/swing/table/TableModel.html
  "
  [target rows]
  (let [target      (to-table-model target)
        col-key-map (get-column-key-map target)]
    (cond
      (nil? rows)     nil
      (integer? rows) (single-value-at target col-key-map rows)
      :else           (map #(single-value-at target col-key-map %) rows))))

(defn update-at!
  "Update a row in a table model or JTable. Accepts an arbitrary number of row/value
  pairs where row is an integer row index and value is a map or vector of values
  just like the :rows property of (table-model).

  Notes:

    Any non-column keys, i.e. keys that weren't present in the original column
    spec when the table-model was constructed will be remembered and retrievable
    later with (value-at).

  Examples:

    ; Given a table created with column keys :a and :b, update row 3 and 5
    (update-at! t 3 [\"Col0 Value\" \"Col1 Value\"]
                  5 { :a \"A value\" \"B value\" })

  See:
    (seesaw.core/table)
    (seesaw.table/table-model)
    http://download.oracle.com/javase/6/docs/api/javax/swing/table/TableModel.html
  "
  ([target row value]
    (let [target      (to-table-model target)
          col-key-map (get-column-key-map target)
          ^objects row-values  (unpack-row col-key-map value)]
      (doseq [i (range 0 (.getColumnCount target))]
        ; TODO this precludes setting a cell to nil. Do we care?
        (let [v (aget row-values i)]
          (when-not (nil? v)
            (.setValueAt target (aget row-values i) row i))))
      ; merge with current full-map value so that extra fields aren't lost.
      (.setValueAt target
                   (merge (.getValueAt target row -1)
                          (last row-values)) row -1))
    target)
  ([target row value & more]
    (when more
      (apply update-at! target more))
    (update-at! target row value)))

(defn insert-at!
  "Inserts one or more rows into a table. The arguments are one or more row-index/value
  pairs where value is either a map or a vector with the right number of columns. Each
  row index indicates the position before which the new row will be inserted. All indices
  are relative to the starting state of the table, i.e. they shouldn't take any shifting
  of rows that takes place during the insert. The indices *must* be in ascending sorted
  order!!

  Returns target.

  Examples:

    ; Insert a row at the front of the table
    (insert-at! 0 {:name \"Agent Cooper\" :likes \"Cherry pie and coffee\"})

    ; Insert two rows, one at the front, one before row 3
    (insert-at! 0 {:name \"Agent Cooper\" :likes \"Cherry pie and coffee\"}
                3 {:name \"Big Ed\"       :likes \"Norma\"})

  "
  ([target ^Integer row value]
    (let [target  (to-table-model target)
          col-key-map (get-column-key-map target)
          ^objects row-values  (unpack-row col-key-map value)]
      (.insertRow target row row-values))
   target)
  ([target row value & more]
    (when more
      (apply insert-at! target more))
    (insert-at! target row value)))

(defn remove-at!
  "Remove one or more rows from a table or table model by index. Args are a list of row indices at
  the start of the operation. The indices *must* be in ascending sorted order!

  Returns target.

  Examples:

    ; Remove first row
    (remove-at! t 0)

    ; Remove first and third row
    (remove-at! t 0 3)
  "
  ([target row]
    (.removeRow (to-table-model target) row)
   target)
  ([target row & more]
    (when more
      (apply remove-at! target more))
    (remove-at! target row)))

(defn clear!
  "Clear all rows from a table model or JTable.

  Returns target.
  "
  [target]
  (.setRowCount (to-table-model target) 0)
  target)

(defn row-count
  "Return number of rows in a table model or JTable."
  [target]
  (.getRowCount (to-table-model target)))

(defn column-count
  "Return number of columns in a table model or JTable."
  [target]
  (.getColumnCount (to-table-model target)))

