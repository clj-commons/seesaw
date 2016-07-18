;   Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns seesaw.test.examples.chooser
  (:use [seesaw core chooser]
        seesaw.test.examples.example)
  (:require [seesaw.dev :as dev]))

(defn make-frame [content]
  (frame :title "Seesaw File Chooser Example"
         :size [300 :by 100]
         :content content))

(defn content []
  (border-panel
    :hgap 5 :vgap 5 :border 5
    :north (label :id :selected-file :text "File wasn't selected yet")
    :center (button :id :choose :text "Choose file")))

(defn update-label [label filename]
  (config! label :text (str "Selected file: " filename)))

(defn add-behavior [f]
  (let [{:keys [selected-file choose]} (group-by-id f)]
    (listen choose :action
      (fn [_] (choose-file
                :dir "./"
                :selected-file "project.clj"
                :success-fn (fn [e f] (update-label selected-file (.getAbsolutePath f)))))))
  f)

(defexample []
  (dev/debug!)
  (-> (make-frame (content)) add-behavior))
