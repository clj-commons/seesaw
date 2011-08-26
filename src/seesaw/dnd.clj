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

(defn- get-import-data 
  [^TransferHandler$TransferSupport support flavor]
  (.. support getTransferable (getTransferData flavor)))

(defn default-transfer-handler 
  "INCOMPLETE!
  Create a transfer handler for drag and drop operations. Take a list
  of key/value option pairs as usual. The following options are supported:
  
    :import - A vector of flavor/handler pairs used when a drop/paste occurs
              (see below)
    :export - A map of options used when a drag/copy occurs (see below)
  
  Data Import

    The :import option specifies a vector of data-flavor/handler pairs. When
    a drop/paste occurs, the handler for the first matching flavor is called
    with a map with the following keys:

      :target        The widget that's the target of the drop
      :data          The data, type depends on flavor
      :drop?         true if this is a drop operation, otherwise it's a paste
      :drop-location Instance of javax.swing.TransferHandler$DropLocation or
                     nil if drop? is false. Use (.getPoint) to get a
                     java.awt.Point.
      :support       Instance of javax.swing.TransferHandler$TransferSupport
                     for advanced use.

    The handler must return truthy if the drop is accepted, falsey otherwise.

  Data Export

    The :export option specifies the behavior when a drag or copy is started
    from a widget. It is a map with the following keys:

      :actions A function that takes a widget and returns a set of supported
               actions. Defaults to #{:move}. Can be any of :move, :copy, or
               :link.
      :start   A function that takes a widget and returns a vector of flavor/value
               pairs to be exported. Defaults to [SomeFlavor (seesaw.core/selection)].
      :finish  A function that takes a map of values. It's called when the drag/paste
               is completed. The map has the following keys:
                :source The widget from which the drag started
                :action The action, :move, :copy, or :link.
                :data   Vector of flavor/value pairs returned by :start function.

  Examples:


    (default-transfer-handler
      ; Allow either strings or lists of files to be dropped
      :import [String       (fn [{:keys [data]}] ... data is a string ...)
               java.io.File (fn [{:keys [data]}] ... data is a *list* of files ...)]
      
      :export {
        :actions (fn [_] #{:move :copy})
        :start   (fn [w] [String (seesaw.core/text w)])
        :finish  (fn [_] ... do something when drag is finished ...) })

  See:

    http://download.oracle.com/javase/6/docs/api/javax/swing/TransferHandler.html
  "
  [& {:keys [import export] :as opts}]
  (let [make-pair        (fn [[flavor handler]] [(to-flavor flavor) handler])
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
