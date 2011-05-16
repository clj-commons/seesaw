;  Copyright (c) Dave Ray, 2011. All ritest/seesaw/test/core.clj

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.table)

(defn- normalize-column [c]
  (cond 
    (map? c) c
    :else {:key c :text (name c)}))

(defn- unpack-row-map [col-key-map row]
  (let [a (object-array (count col-key-map))]
    (doseq [[k v] row]
      (aset a (get col-key-map k) v))
    a))

(defn- unpack-row [col-key-map row]
  (cond
    (map? row) (unpack-row-map col-key-map row)
    :else      (object-array row)))

(defn table-model
  "Creates a TableModel from column and row data. Takes two options:

    :columns - a list of keys, or maps. If a key, then (name key) is used
               as the column name. If a map, it must be in the form
               {:key key :text text} where text is used as the column name
               and key is use to index the row data.
               The order establishes the order of the columns in the table.

    :rows - a sequence of maps or vectors, possibly mixed. If a map, must contain
            row data indexed by keys in :columns. If a vector, data is indexed
            by position in the vector.

  Example:
    
    (table-model :columns [:name 
                           {:key :age :text \"Age\"}]
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
        col-names   (object-array (map :text norm-cols))
        col-key-map (reduce (fn [m [k v]] (assoc m k v)) {} (map-indexed #(vector (:key %2) %1) norm-cols))
        model     (javax.swing.table.DefaultTableModel. col-names 0)]
    (doseq [row rows]
      (.addRow model (unpack-row col-key-map row)))
    model))


