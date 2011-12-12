;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.explorer
  (:use [seesaw core tree]
        seesaw.test.examples.example)
  (:import [java.io File]
           [javax.swing.filechooser FileSystemView]))

; Make a model for the directory tree
(def tree-model
  (simple-tree-model
    #(.isDirectory %)
    (fn [f] (filter #(.isDirectory %) (.listFiles f)))
    (File. ".")))

(def chooser (javax.swing.JFileChooser.)) ; FileChooser hack to get system icons

; Render thing with right names and system icons
(defn render-file-item
  [renderer {:keys [value]}]
  (config! renderer :text (.getName value)
                   :icon (.getIcon chooser value)))

(defn make-frame []
  ; Put all the widgets together
  (frame :title "File Explorer" :width 500 :height 500 
    :content
    (border-panel :border 5 :hgap 5 :vgap 5
      :north  (label :id :current-dir :text "Location")

      :center (left-right-split
                (scrollable (tree    :id :tree :model tree-model :renderer render-file-item))
                (scrollable (listbox :id :list :renderer render-file-item))
                :divider-location 1/3)

      :south  (label :id :status :text "Ready"))))

(defexample []
  (let [f (make-frame)]
    ; Hook up a selection listener to the tree to update stuff
    (listen (select f [:#tree]) :selection
      (fn [e]
        (if-let [dir (last (selection e))]
          (let [files (.listFiles dir)]
            (config! (select f [:#current-dir]) :text (.getAbsolutePath dir))
            (config! (select f [:#status]) :text (format "Ready (%d items)" (count files)))
            (config! (select f [:#list]) :model files)))))
    f))

;(run :dispose)

