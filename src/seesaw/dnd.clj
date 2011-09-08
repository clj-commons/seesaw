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
  (:require clojure.set
            clojure.string)
  (:import [java.awt.datatransfer DataFlavor
                                  UnsupportedFlavorException
                                  Transferable]
           [javax.swing TransferHandler 
                        TransferHandler$TransferSupport]))

(defprotocol Flavorful
  "Protocol for abstracting DataFlavor including automatic conversion from
  external/native representations (e.g. uri-list) to friendlier internal 
  representations (e.g. list of java.net.URL)."
  (to-raw-flavor [this]
    "Return an instance of java.awt.datatransfer.DataFlavor for this.")
  (to-local [this value] 
    "Given an incoming value convert it to the expected local format. For example, a uri-list
    would return a vector of URL.")
  (to-remote [this value] 
    "Given an outgoing value, convert it to the appropriate remote format.
    For example, a vector of URLs would be serialized as a uri-list."))

; Default/do-nothin impl for DataFlavors
(extend-protocol Flavorful
  DataFlavor
  (to-raw-flavor [this] this)
  (to-local [this value] value)
  (to-remote [this value] value))

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

(defn local-object-flavor 
  "Creates a flavor for moving raw Java objects between components within a
  single JVM instance. class-or-value is either the class of data, or an
  example value from which the class is taken.
  
  Examples:
  
    ; Move Clojure vectors
    (local-object-flavor [])
  "
  [class-or-value]
  (if (class? class-or-value)
    (make-flavor DataFlavor/javaJVMLocalObjectMimeType class-or-value)
    (local-object-flavor (class class-or-value))))

(def ^{:doc "Flavor for a list of java.io.File objects" }
  file-list-flavor DataFlavor/javaFileListFlavor)

(def ^{:doc "Flavor for a list of java.net.URL objects." }
  uri-list-flavor 
  (let [flavor (make-flavor "text/uri-list" String)]
    (reify Flavorful
      (to-raw-flavor [this] flavor)
      (to-local [this value] 
        (map #(java.net.URL. %) (clojure.string/split-lines value)))
      (to-remote [this value] 
        (clojure.string/join "\r\n" (map #(.toExternalForm ^java.net.URL %) value))))))

(def ^{:doc "Flavor for HTML text"} html-flavor (make-flavor "text/html" String))
(def ^{:doc "Flavor for images as java.awt.Image" } image-flavor DataFlavor/imageFlavor)
(def ^{:doc "Flavor for raw text" } string-flavor DataFlavor/stringFlavor)

(defn default-transferable 
  "Constructs a transferable given a vector of alternating flavor/value pairs.
  If a value is a function, i.e. (fn? value) is true, then then function is 
  called with no arguments when the value is requested for its corresponding 
  flavor. This way calculation of the value can be deferred until drop time. 
  
  Each flavor must be unique and it's assumed that the flavor and value agree.
  
  Examples:
  
    ; A transferable holding String or File data where the file calc is
    ; deferred
    (default-transferable [string-flavor    \"/home/dave\"
                           file-list-flavor #(vector (java.io.File. \"/home/dave\"))])
  
  "
  [pairs]
  (let [pairs      (map (fn [[f v]] [(to-raw-flavor f) [f v]]) (partition 2 pairs))
        flavor-map (into {} pairs)
        flavor-arr (into-array DataFlavor (map first pairs))]
    (proxy [Transferable] []
      (isDataFlavorSupported [flavor]
        (contains? flavor-map flavor))

      (getTransferDataFlavors [] flavor-arr)

      (getTransferData [flavor]
        (if-let [[flavorful v] (get flavor-map flavor)]
          (to-remote flavorful (if (fn? v) (v) v))
          (throw (UnsupportedFlavorException. flavor)))))))

(defn- get-import-handler
  [^TransferHandler$TransferSupport support pairs]
  (some
    (fn [[flavorful handler :as v]]
      (if (.isDataFlavorSupported support (to-raw-flavor flavorful))
        v))
    pairs))

(defn- get-import-data 
  [^TransferHandler$TransferSupport support flavorful]
  (to-local flavorful (.. support getTransferable (getTransferData (to-raw-flavor flavorful)))))

(def ^{:private true} keyword-to-action
  (constant-map TransferHandler :copy :copy-or-move :link :move :none))

(def ^{:private true} action-to-keyword 
  (clojure.set/map-invert keyword-to-action))

(defn- unpack-drop-location [^javax.swing.TransferHandler$DropLocation dl]
  (let [^java.awt.Point pt (.getDropPoint dl) ]
    (merge 
      (cond
        (instance? javax.swing.JList$DropLocation dl) 
          { :index (.getIndex dl) :insert? (.isInsert dl) }

        (instance? javax.swing.JTable$DropLocation dl)
          { :column (.getColumn dl)
            :row    (.getRow dl)
            :insert-column? (.isInsertColumn dl)
            :insert-row?    (.isInsertRow dl) }
        (instance? javax.swing.text.JTextComponent$DropLocation dl)
          { :bias (.getBias dl)
            :index (.getIndex dl) }
        (instance? javax.swing.JTree$DropLocation dl)
          { :index (.getChildIndex dl) 
            :path  (.getPath dl) }
        :else {})
      {:point [(.x pt) (.y pt)]})))

(defn default-transfer-handler 
  "Create a transfer handler for drag and drop operations. Take a list
  of key/value option pairs as usual. The following options are supported:
  
    :import - A vector of flavor/handler pairs used when a drop/paste occurs
              (see below)
    :export - A map of options used when a drag/copy occurs (see below)
  
  Data Import

    The :import option specifies a vector of flavor/handler pairs. When
    a drop/paste occurs, the handler for the first matching flavor is called
    with a map with the following keys:

      :target        The widget that's the target of the drop
      :data          The data, type depends on flavor
      :drop?         true if this is a drop operation, otherwise it's a paste
      :drop-location Map of drop location info or nil if drop? is false. See 
                     below.
      :support       Instance of javax.swing.TransferHandler$TransferSupport
                     for advanced use.

    The handler must return truthy if the drop is accepted, falsey otherwise.

    If :drop? is true, :drop-location will be non-nil and include the following
    keys, depending on the type of the drop target:

      All types:

        :point    [x y] vector

      listbox 
  
        :index    The index for the drop
        :insert?  True if it's an insert, i.e. \"between\" entries

      table 

        :column         The column for the drop
        :row            The row for the drop
        :insert-column? True if it's an insert, i.e. \"between\" columns.
        :insert-row?    True if it's an insert, i.e. \"between\" rows 

      tree 
  
        :index  The index of the drop point 
        :path   TreePath of the drop point

      Text Components

        :bias   No idea what this is
        :index  The insertion index

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
      :import [string-flavor    (fn [{:keys [data]}] ... data is a string ...)
               file-list-flavor (fn [{:keys [data]}] ... data is a *list* of files ...)]
      
      :export {
        :actions (fn [_] :copy)
        :start   (fn [w] [string-flavor (seesaw.core/text w)])
        :finish  (fn [_] ... do something when drag is finished ...) })

  See:

    http://download.oracle.com/javase/6/docs/api/javax/swing/TransferHandler.html
  "
  [& {:keys [import export] :as opts}]
  (let [import-pairs     (partition 2 import)
        accepted-flavors (map (comp to-raw-flavor first) import-pairs)
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
          (try 
            (let [[flavorful handler] (get-import-handler support import-pairs)
                  data                         (get-import-data support flavorful)
                  drop?                        (.isDrop support)]
              (boolean (handler { :data          data 
                                  :drop?         drop?
                                  :drop-location (if drop? (unpack-drop-location (.getDropLocation support)))
                                  :target        (.getComponent support)
                                  :support       support })))
            ; When Swing calls importData it seems to catch and suppress all 
            ; exceptions, which is maddening to debug. :|
            (catch Exception e 
              (.printStackTrace e) 
              (throw e)))
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

