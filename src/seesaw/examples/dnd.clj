;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.dnd
  (:use seesaw.core)
  (:require [seesaw.dnd :as dnd]))

; From http://download.oracle.com/javase/tutorial/uiswing/examples/dnd/ListCutPasteProject/src/dnd/ListCutPaste.java

(defn transfer-handler []
  (dnd/default-transfer-handler
    :import [String (fn [{:keys [target data drop? drop-location] }]
                      (let [model (.getModel target)]
                        (if drop?
                          (let [dl drop-location
                                index (.getIndex dl)]
                            (if (.isInsert dl)
                              (.add model index data)
                              (.set model index data)))
                          (let [index (.getSelectedIndex target)]
                            (if (neg? index)
                              (.addElement model data)
                              (.add model (inc index) data))))))]
    :export {
      :actions (constantly :copy-or-move)
      :start   (fn [c] [String (selection c)])
      :finish  (fn [{:keys [source action]}]
                 (if (= action :move)
                   (.. source getModel (remove (.getSelectedIndex source))))) }))

(defn app []
  (let [th    (transfer-handler)
        list1 (listbox 
                :drag-enabled? true
                :drop-mode :on-or-insert
                :transfer-handler th
                :preferred-size [100 :by 400]
                :selection-mode :single
                :model ["alpha" "beta" "gamma" "delta" "epsilon" "zeta"])
        list2 (listbox 
                :drag-enabled? true
                :drop-mode :insert
                :transfer-handler th
                :preferred-size [100 :by 400]
                :selection-mode :single
                :model ["uma" "dois" "tres" "quatro" "cinco" "seis"])
        list3 (listbox 
                :drag-enabled? true
                :drop-mode :on
                :preferred-size [100 :by 400]
                :selection-mode :single
                :model ["adeen" "dva" "tri" "chyetirye" "pyat" "shest"])]
    (frame
      :title "ListCutPaste"
      :content
        (grid-panel 
          :columns 3
          :items [ 
            (border-panel :border "Greek Alphabet" :center (scrollable list1))
            (border-panel :border "Portuguese Numbers" :center (scrollable list2))
            (border-panel :border "Russian Numbers" :center (scrollable list3))]))))

(defn -main [& args]
  (invoke-later
    (-> (app)
      pack!
      show!)))

 ;(-main)
