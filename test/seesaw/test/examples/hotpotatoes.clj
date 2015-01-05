;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.hotpotatoes
  (:use seesaw.core seesaw.util seesaw.font
        seesaw.test.examples.example)
  (:require [clojure.java.io :only reader]))

; A simple HTTP request app. Enter a URL and click "Go". It does the request in
; the background and displays the response.

(defn do-request [url-str f]
  (future
    (let [result (if-let [url (to-url url-str)]
                  (slurp url)
                  "Invalid URL")]
      (invoke-later (f result)))))

(defexample []
  (let [exit-action (action :handler dispose! :name "Exit")
        url-text    (text "http://google.com")
        status      (label "Ready")
        result-text (text :multi-line? true :editable? false :font "MONOSPACED-14")
        result-handler (fn [s]
                         (text! result-text s)
                         (text! status "Ready"))
        go-handler (fn [e]
                     (text! status "Busy")
                     (do-request
                       (text url-text)
                       result-handler))]
    (frame
      :id :frame
      :title "Hot Potatoes!"
      :menubar (menubar :items [(menu :text "File" :items [exit-action])])
      :width 500 :height 600
      :content
        (border-panel
          :border 5
          :north (toolbar :items [exit-action])
          :center
            (border-panel
              :north
                (horizontal-panel
                  :border [5 "Configure Request"]
                  :items ["URL" url-text
                          (action :handler go-handler :name "Go")])
              :center
                (horizontal-panel
                  :border [5 "Request Result"]
                  :items [(scrollable result-text)]))
          :south status))))

;(run :dispose)

