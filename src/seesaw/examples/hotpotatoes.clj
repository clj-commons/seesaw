(ns seesaw.examples.hotpotatoes
  (:use seesaw.core seesaw.util seesaw.font)
  (:require [clojure.java.io :only reader]))

; A simple HTTP request app. Enter a URL and click "Go". It does the request in 
; the background and displays the response.

(defn do-request [url-str f]
  (future 
    (let [result (if-let [url (to-url url-str)]
                  (slurp url)
                  ("Invalid URL"))]
      (invoke-later #(f result)))))
      
(defn app 
  [exit?]
  (let [exit-action (action (fn [e] (if exit? (System/exit 0) (.dispose (to-frame e)))) :name "Exit")
        url-text    (text "http://google.com")
        status      (label "Ready")
        result-text (text :multi-line? true :editable? false :font "MONOSPACED-14")
        result-handler (fn [s]
                         (.setText result-text s)
                         (.setText status "Ready"))
        go-handler (fn [e] 
                     (.setText status "Busy")
                     (do-request 
                       (.getText url-text)
                       result-handler))]
    (frame 
      :title "Hot Potatoes!"
      :width 500 :height 600
      :pack? false ; So width and height have an effect
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
                          (action go-handler :name "Go")])
              :center
                (horizontal-panel 
                  :border [5 "Request Result"]
                  :items [(scrollable result-text)]))
          :south status))))

(defn -main [& args]
  (invoke-later #(app true)))

