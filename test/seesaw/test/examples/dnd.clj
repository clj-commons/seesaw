;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.dnd
  (:use seesaw.core
        seesaw.test.examples.example)
  (:require [seesaw.dnd :as dnd]))

; Set up a few targets for different data flavors.

; A list box that imports and exports strings
(defn string-target []
  (listbox
    :model ["A String"]
    :drag-enabled? true
    :drop-mode :insert
    :transfer-handler 
      [:import [dnd/string-flavor (fn [{:keys [target data]}]
                                    (.. target getModel (addElement data)))]
       :export {
         :actions (constantly :copy)
         :start   (fn [c] [dnd/string-flavor (selection c)])
         ; No :finish needed
        }]))


; A list box that imports and exports files, like with finder or explorer
(defn file-target []
  (listbox
    :model []
    :drag-enabled? true
    :drop-mode :insert
    :transfer-handler 
      (dnd/default-transfer-handler 
        :import [dnd/file-list-flavor (fn [{:keys [target data]}]
                                        ; data is always List<java.io.File>
                                        (doseq [file data] 
                                          (.. target getModel (addElement file))))]
        :export {
          :actions (constantly :copy)
          :start   (fn [c] 
                     (let [file (selection c)] 
                       [dnd/file-list-flavor [file]]))
          ; No :finish needed
        })))

; A list box that imports and exports URIs, like with a browser
(defn url-target []
  (listbox
    :model [(java.net.URI. "http://github.com/daveray/seesaw")]
    :drag-enabled? true
    :drop-mode :insert
    :transfer-handler 
      (dnd/default-transfer-handler 
        :import [dnd/uri-list-flavor (fn [{:keys [target data]}]
                                      ; data is seq of java.net.URI
                                      (doseq [url data] 
                                        (.. target getModel (addElement url))))]
        :export {
          :actions (constantly :copy)
          :start   (fn [c] 
                     (let [url (selection c)] 
                       [dnd/uri-list-flavor [url] ]))
          ; No :finish needed
        })))

; A list box that imports and exports images, like with a browser
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
              :import [dnd/image-flavor (fn [{:keys [target data]}]
                                        (.. target getModel (addElement data)))])))
      (scrollable icon-label)
      :divider-location 1/2)))

(defexample []
  (frame
    :title "Seesaw Drag-n-Drop Example"
    :content
      (vertical-panel
        :items [ 
          (border-panel :border "Drag and Drop Text here"   :center (scrollable (string-target)))
          (border-panel :border "Drag and Drop Files here"  :center (scrollable (file-target)))
          (border-panel :border "Drag and Drop URIs here"   :center (scrollable (url-target)))
          (border-panel :border "Drag and Drop Images here" :center (image-target))
          ])))

;(run :dispose)

