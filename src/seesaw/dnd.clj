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
  (:use [seesaw.util :only [constant-map]])
  (:require clojure.set)
  (:import [java.awt.datatransfer DataFlavor
                                  UnsupportedFlavorException
                                  Transferable]
           [javax.swing TransferHandler 
                        TransferHandler$TransferSupport]))

(defn ^DataFlavor make-flavor
  "Construct a new data flavor with the given mime-type and representation class.
  

  Notes:

    Run seesaw.dnd-explorer to experiment with flavors coming from
    other apps.

  Examples:
  
    ; HTML as a reader
    (make-flavor \"text/html\" java.io.Reader)
  "
  [mime-type ^java.lang.Class rep-class]
  (DataFlavor. 
    (format "%s;class=%s" 
            mime-type 
            (.getCanonicalName rep-class))))

(def url-flavor (make-flavor "application/x-java-url" java.net.URL))
(def html-flavor (make-flavor "text/html" java.lang.String))

(defn ^DataFlavor to-flavor
  [v]
  (cond
    (instance? DataFlavor v) v
    (= v String) DataFlavor/stringFlavor
    (= v java.io.File) DataFlavor/javaFileListFlavor
    (= v java.net.URL) url-flavor
    (= v java.io.File) DataFlavor/javaFileListFlavor
    (= v java.awt.Image) DataFlavor/imageFlavor
    (instance? java.awt.Image v) DataFlavor/imageFlavor
    (class? v) (DataFlavor. (format "%s; class=%s" DataFlavor/javaJVMLocalObjectMimeType (.getName v)))

    :else (to-flavor (class v))))

(defn default-transferable 
  "Constructs a transferable given a vector of alternating flavor/value pairs.
  Flavors are passed through (seesaw.dnd.to-flavor). If a value is a function,
  i.e. (fn? value) is true, then then function is called with no arguments
  when the value is requested for its corresponding flavor. This way calculation
  of the value can be deferred until drop time. 
  
  Each flavor must be unique and it's assumed that the flavor and value agree.
  
  Examples:
  
    ; A transferable holding String or File data where the file calc is
    ; deferred
    (default-transferable [String       \"/home/dave\"
                           java.io.File (fn [] (java.io.File. \"/home/dave\"))])
  
  "
  [pairs]
  (let [pairs (map 
                (fn [[f v]] 
                  [(to-flavor f) v]) 
                (partition 2 pairs))
        flavor-map (into {} pairs)
        flavor-arr (into-array DataFlavor (map first pairs))]
    (proxy [Transferable] []
      (isDataFlavorSupported [flavor]
        (contains? flavor-map flavor))

      (getTransferDataFlavors [] flavor-arr)

      (getTransferData [flavor]
        (if-let [v (get flavor-map flavor)]
          (if (fn? v) (v) v)
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

(def ^{:private true} keyword-to-action
  (constant-map TransferHandler :copy :copy-or-move :link :move :none))

(def ^{:private true} action-to-keyword 
  (clojure.set/map-invert keyword-to-action))

(defn default-transfer-handler 
  "Create a transfer handler for drag and drop operations. Take a list
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

      :actions A function that takes a widget and returns a keyword indicating
               supported actions. Defaults to :move. Can be any of :move, :copy, 
               :copy-or-move, :link, or :none.
      :start   A function that takes a widget and returns a vector of flavor/value
               pairs to be exported. Required.
      :finish  A function that takes a map of values. It's called when the drag/paste
               is completed. The map has the following keys:
                :source The widget from which the drag started
                :action The action, :move, :copy, or :link.
                :data   A Transferable

  Examples:


    (default-transfer-handler
      ; Allow either strings or lists of files to be dropped
      :import [String       (fn [{:keys [data]}] ... data is a string ...)
               java.io.File (fn [{:keys [data]}] ... data is a *list* of files ...)]
      
      :export {
        :actions (fn [_] :copy)
        :start   (fn [w] [String (seesaw.core/text w)])
        :finish  (fn [_] ... do something when drag is finished ...) })

  See:

    http://download.oracle.com/javase/6/docs/api/javax/swing/TransferHandler.html
  "
  [& {:keys [import export] :as opts}]
  (let [make-pair        (fn [[flavor handler]] [(to-flavor flavor) handler])
        import-pairs     (map make-pair (partition 2 import))
        accepted-flavors (map first import-pairs)
        start            (if-let [start-val (:start export)]
                           (fn [c] (default-transferable (start-val c))))
        finish           (:finish export)
        actions          (if export 
                             (or (:actions export) (constantly :move))
                             (constantly :none))] 
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
          false))

      (createTransferable [^javax.swing.JComponent c]
        (start c))

      (getSourceActions [^javax.swing.JComponent c]
        (keyword-to-action (or (actions c) :none)))

      (exportDone [^javax.swing.JComponent c ^Transferable data action]
        (if finish
          (finish { :source c 
                             :data   data 
                             :action (action-to-keyword action) }))))))



(defn ^TransferHandler to-transfer-handler
  [v]
  (cond
    (instance? TransferHandler v) v
    (vector? v) (apply default-transfer-handler v)
    :else (throw (IllegalArgumentException. (str "Don't know how to make TransferHandler from: " v)))))

(defn everything-transfer-handler 
  "Handler that accepts all drops. For debugging."
  [handler]
  (proxy [TransferHandler] []

    (canImport [^TransferHandler$TransferSupport support] true)

    (importData [^TransferHandler$TransferSupport support]
      (handler support)
      false)

    (createTransferable [^javax.swing.JComponent c] nil)

    (getSourceActions [^javax.swing.JComponent c] TransferHandler/NONE)

    (exportDone [^javax.swing.JComponent c ^Transferable data action])))

