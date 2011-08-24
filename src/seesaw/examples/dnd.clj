;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.dnd
  (:use seesaw.core))

; From http://download.oracle.com/javase/tutorial/uiswing/examples/dnd/ListCutPasteProject/src/dnd/ListCutPaste.java

(defn string-data [info]
  (try
    (.. info getTransferable (getTransferData java.awt.datatransfer.DataFlavor/stringFlavor))
    (catch Exception e nil)))

(defn transfer-handler []
  (proxy [javax.swing.TransferHandler] []
    (importData [^javax.swing.TransferHandler$TransferSupport info]
      (when (.canImport this info)
        (let [list  (.getComponent info)
              model (.getModel list)
              data  (string-data info)]
          (if data
            (if (.isDrop info)
              (let [dl (.getDropLocation info)
                    index (.getIndex dl)]
                (if (.isInsert dl)
                  (.add model index data)
                  (.set model index data)))
              (let [index (.getSelectedIndex list)]
                (if (neg? index)
                  (.addElement model data)
                  (.add model (inc index) data))))
            true))))

    (createTransferable [^javax.swing.JComponent c]
      (java.awt.datatransfer.StringSelection. (selection c)))
    
    (getSourceActions [^javax.swing.JComponent c]
      javax.swing.TransferHandler/COPY_OR_MOVE)
    
    (exportDone [^javax.swing.JComponent c ^javax.swing.Transferable data action]
      (if (= javax.swing.TransferHandler/MOVE)
        (.. c getModel (remove (.getSelectedIndex c)))))
    
    (canImport [^javax.swing.TransferHandler$TransferSupport support]
      (.isDataFlavorSupported support java.awt.datatransfer.DataFlavor/stringFlavor))))

(defn app []
  (let [th    (transfer-handler)
        list1 (doto (listbox 
                      :preferred-size [100 :by 400]
                      :selection-mode :single
                      :model ["alpha" "beta" "gamma" "delta" "epsilon" "zeta"])
                (.setDragEnabled true)
                (.setDropMode javax.swing.DropMode/ON_OR_INSERT)
                (.setTransferHandler th))
        list2 (doto (listbox 
                      :preferred-size [100 :by 400]
                      :selection-mode :single
                      :model ["uma" "dois" "tres" "quatro" "cinco" "seis"])
                (.setDragEnabled true)
                (.setDropMode javax.swing.DropMode/INSERT)
                (.setTransferHandler th))
        list3 (doto (listbox 
                      :preferred-size [100 :by 400]
                      :selection-mode :single
                      :model ["adeen" "dva" "tri" "chyetirye" "pyat" "shest"])
                (.setDragEnabled true)
                (.setDropMode javax.swing.DropMode/ON)
                (.setTransferHandler th))]
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
