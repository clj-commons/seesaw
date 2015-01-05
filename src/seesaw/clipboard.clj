(ns seesaw.clipboard
  (:require [seesaw.dnd :as dnd]))

(defn ^java.awt.datatransfer.Clipboard system
  []
  (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit)))

(defn contents
  "Retrieve the current content of the system clipboard in the given flavor.
  If omitted, flavor defaults to seesaw.dnd/string-flavor. If not content
  with the given flavor is found, returns nil.

  See:
    seesaw.dnd
    http://docs.oracle.com/javase/7/docs/api/java/awt/datatransfer/Clipboard.html
  "
  ([] (contents dnd/string-flavor))
  ([flavor]
    (try
      (.getData (system) (dnd/to-raw-flavor flavor))
      (catch java.awt.datatransfer.UnsupportedFlavorException e nil))))

(defn ^java.awt.datatransfer.Clipboard contents!
  "Set the content of the sytem clipboard to the given transferable. If
  transferable is a string, a string transferable is created. Otherwise,
  use seesaw.dnd/default-transferable to create one.

  Returns the clipboard.

  See:
    (seesaw.dnd/default-transferable)
    http://docs.oracle.com/javase/7/docs/api/java/awt/datatransfer/Clipboard.html
  "
  ([transferable]
    (contents! transferable nil))
  ([transferable ^java.awt.datatransfer.ClipboardOwner owner]
   (let [cb (system)]
     (cond
       (string? transferable)
         (contents! (dnd/default-transferable [dnd/string-flavor transferable]) owner)
     :else
       (.setContents (system) ^java.awt.datatransfer.Transferable transferable owner))
     cb)))

(comment
  (contents! (dnd/default-transferable [dnd/string-flavor     "/home/dave"
                                        dnd/file-list-flavor #(vector (java.io.File.  "/home/dave"))]))
  (contents)
  (contents dnd/file-list-flavor))
