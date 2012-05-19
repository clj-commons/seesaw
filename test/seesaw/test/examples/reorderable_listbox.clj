;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.reorderable-listbox
  (:use seesaw.core
        seesaw.test.examples.example)
  (:require [seesaw.dnd :as dnd]))


(defn list-with-elem-at-index
  "Given a sequence cur-order and elem-to-move is one of the items
within it, return a vector that has all of the elements in the same
order, except that elem-to-move has been moved to just before the
index new-idx.

Examples:
user=> (def l [\"a\" \"b\" \"c\" \"d\"])
user=> (list-with-elem-at-index l \"b\" 0)
[\"b\" \"a\" \"c\" \"d\"]
user=> (list-with-elem-at-index l \"b\" 1)
[\"a\" \"b\" \"c\" \"d\"]
user=> (list-with-elem-at-index l \"b\" 2)
[\"a\" \"b\" \"c\" \"d\"]
user=> (list-with-elem-at-index l \"b\" 3)
[\"a\" \"c\" \"b\" \"d\"]
user=> (list-with-elem-at-index l \"b\" 4)
[\"a\" \"c\" \"d\" \"b\"]"
  [cur-order elem-to-move new-idx]
  (let [cur-order (vec cur-order)
        cur-idx (.indexOf cur-order elem-to-move)]
    (if (= new-idx cur-idx)
      cur-order
      (if (< new-idx cur-idx)
        (vec (concat (subvec cur-order 0 new-idx)
                     [ elem-to-move ]
                     (subvec cur-order new-idx cur-idx)
                     (subvec cur-order (inc cur-idx))))
        ;; else new-idx > cur-idx
        (vec (concat (subvec cur-order 0 cur-idx)
                     (subvec cur-order (inc cur-idx) new-idx)
                     [ elem-to-move ]
                     (subvec cur-order new-idx)))))))


(defn reorderable-listbox
  "A listbox of items that the user can reorder by dragging and
dropping.  The caller provide as input an atom containing a sequence
of immutable data values, e.g. strings.  That sequence will give the
original order that items appear in the list.  The atom contents will
be changed to a new sequence whenever the user modifies the order.  No
new items are allowed to be added, nor may existing items be removed."
  [item-list-atom]
  (let [item-list @item-list-atom
        item-set (set item-list)]
    (listbox :model item-list
             :drag-enabled? true
             :drop-mode :insert
             :transfer-handler
             (dnd/default-transfer-handler
               :import [dnd/string-flavor
                        (fn [{:keys [target data drop? drop-location] :as m}]
                          ;; Ignore anything dropped onto the list
                          ;; that is not in the original set of list
                          ;; items.
                          (if (and drop?
                                   (:insert? drop-location)
                                   (:index drop-location)
                                   (item-set data))
                            (let [new-order (list-with-elem-at-index
                                              @item-list-atom data
                                              (:index drop-location))]
                              (reset! item-list-atom new-order)
                              (config! target :model new-order))))]
               :export {:actions (constantly :copy)
                        :start   (fn [c]
                                   [dnd/string-flavor (selection c)])}))))


(defexample []
  (let [atom-with-cur-item-order (atom ["Pie" "Cake" "Cookies"
                                        "Ice Cream" "Donut"])]
    (frame
     :title "Example of listbox with reorderable items"
     :content
     (reorderable-listbox atom-with-cur-item-order))))
