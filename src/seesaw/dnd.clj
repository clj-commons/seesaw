;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for dealing with drag and drop and data transfer."
      :author "Dave Ray"}
  seesaw.dnd
  (:import [java.awt.datatransfer DataFlavor
                                  UnsupportedFlavorException
                                  Transferable]
           [javax.swing TransferHandler 
                        TransferHandler$TransferSupport]))

(defn ^DataFlavor to-flavor
  [v]
  (cond
    (instance? DataFlavor v) v
    (= v String) DataFlavor/stringFlavor
    (= v java.io.File) DataFlavor/javaFileListFlavor
    (= v java.awt.Image) DataFlavor/imageFlavor
    (instance? java.awt.Image v) DataFlavor/imageFlavor
    (class? v) (DataFlavor. (format "%s; class=%s" DataFlavor/javaJVMLocalObjectMimeType (.getName v)))

    :else (to-flavor (class v))))

(defn default-transferable 
  [o]
  (let [flavors (into-array DataFlavor [(to-flavor o)])]
    (proxy [Transferable] []
      (isDataFlavorSupported [flavor]
        (= flavor (aget flavors 0)))

      (getTransferDataFlavors [] flavors)

      (getTransferData [flavor]
        (if (.isDataFlavorSupported this flavor)
          o
          (throw (UnsupportedFlavorException. flavor)))))))

(defn- get-import-handler
  [^TransferHandler$TransferSupport support pairs]
  (some
    (fn [[flavor handler :as v]]
      (if (.isDataFlavorSupported support flavor)
        v))
    pairs))

(defn- get-import-data [^TransferHandler$TransferSupport support flavor]
  (.. support getTransferable (getTransferData flavor)))

(defn default-transfer-handler 
  [& {:keys [import export] :as opts}]
  (let [import           (if (map? import) (mapcat vec import) import)
        make-pair        (fn [[flavor handler]] [(to-flavor flavor) handler])
        import-pairs     (map make-pair (partition 2 import))
        accepted-flavors (map first import-pairs)] 
    (proxy [TransferHandler] []

      (canImport [^TransferHandler$TransferSupport support]
        (boolean (some #(.isDataFlavorSupported support %) accepted-flavors)))

      (importData [^TransferHandler$TransferSupport support]
        (if (.canImport this support)
          (let [[^DataFlavor flavor handler] (get-import-handler support import-pairs)
                data                         (get-import-data support flavor)
                drop?                        (.isDrop support)]
            (boolean (handler { :data          data 
                                :drop?         drop?
                                :drop-location (if drop? (.getDropLocation support))
                                :target        (.getComponent support)
                                :support       support })))
          false)))))
