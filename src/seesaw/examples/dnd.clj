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

; Set up a few targets for different data flavors.

(defn string-target []
  (listbox
    :model ["A String"]
    :drag-enabled? true
    :drop-mode :insert
    :transfer-handler 
      (dnd/default-transfer-handler 
        :import [String (fn [{:keys [target data]}]
                          (.. target getModel (addElement data)))]
        :export {
          :actions (constantly :copy)
          :start   (fn [c] [String (selection c)])
          ; No :finish needed
        })))

(defn file-target []
  (listbox
    :model []
    :drag-enabled? true
    :drop-mode :insert
    :transfer-handler 
      (dnd/default-transfer-handler 
        :import [java.io.File (fn [{:keys [target data]}]
                                ; For File flavor, data is always List<java.io.File>
                                (doseq [file data] 
                                  (.. target getModel (addElement file))))])))
(defn url-target []
  (listbox
    :model [(java.net.URL. "http://github.com/daveray/seesaw")]
    :drag-enabled? true
    :drop-mode :insert
    :transfer-handler 
      (dnd/default-transfer-handler 
        :import [java.net.URL (fn [{:keys [target data]}]
                                ; data is java.net.URL
                                (.. target getModel (addElement data)))]
        :export {
          :actions (constantly :copy)
          :start   (fn [c] 
                     (let [url (selection c)] 
                       [java.net.URL        url
                        ; Most apps (browsers) want uri-list flavor which is a string of 
                        ; CRLF-delimited URLs 
                        dnd/uri-list-flavor (.toExternalForm url)]))
          ; No :finish needed
        })))

(defn image-target []
  (let [icon-label (label)] 
    (left-right-split
      (scrollable
        (listbox
          :listen [:selection (fn [e] (config! icon-label :icon (selection e)))]
          :selection-mode :single
          :model []
          :drag-enabled? true
          :drop-mode :insert
          :transfer-handler 
            (dnd/default-transfer-handler 
              :import [java.awt.Image (fn [{:keys [target data]}]
                                        (.. target getModel (addElement data)))])))
      (scrollable icon-label)
      :divider-location 1/2)))

(defn app []
  (frame
    :title "Seesaw Drag-n-Drop Example"
    :content
      (vertical-panel
        :items [ 
          (border-panel :border "Drag and Drop Text here" :center (scrollable (string-target)))
          (border-panel :border "Drag and Drop Files here" :center (scrollable (file-target)))
          (border-panel :border "Drag and Drop URLs here" :center (scrollable (url-target)))
          (border-panel :border "Drag and Drop Images here" :center (image-target))
          ])))

(defn -main [& args]
  (invoke-later
    (-> (app)
      pack!
      show!)))

;(-main)
