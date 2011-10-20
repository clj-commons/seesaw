;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns gaidica.core
  (:require [ororo.core :as storm])
  (:use [seesaw core border table mig]))

(native!)

(def api-key (atom ""))
(def normal-font "ARIAL-12-PLAIN")
(def title-font "ARIAL-14-BOLD")
(def divider-color "#aaaaaa")

(defn get-text-forecasts [city]
  (get-in (storm/forecast @api-key city) [:txt_forecast :forecastday]))

(defn get-webcams [city] (storm/webcams @api-key city))

(defn make-forecast-entry [f]
  (mig-panel 
    :constraints ["" "[][grow, fill]" "[top][top]"]
    :border (line-border :bottom 1 :color divider-color)
    :items [
      [(label :icon (:icon_url f))                   "span 1 2"]
      [(label :text (:title f) :font title-font) "wrap"]
      [(text 
         :opaque? false 
         :multi-line? true 
         :wrap-lines? true 
         :editable? false 
         :text (:fcttext f)
         :font normal-font)                         "wmin 10"]]))

(defn make-forecast-entries [forecasts]
  (map #(vector (make-forecast-entry %) "wrap") forecasts))

(defn make-forecast-panel [forecasts]
  (mig-panel 
    :id :forecast
    :constraints ["" "[grow, fill]"] 
    :items (make-forecast-entries forecasts)))

(defn update-forecasts [forecast-panel forecasts]
  (config! forecast-panel :items (make-forecast-entries forecasts)))

(defn make-webcam-table [] 
  (table 
    :id :webcam-table 
    :model 
      [:columns 
        [{:key :handle :text "Name" } 
         :lat :lon 
         {:key :updated :text "Last Updated"}
         {:key :CURRENTIMAGEURL :text "Image"}]]))

(defn make-webcam-panel []
  (let [webcam-table (make-webcam-table)
        image-label  (label :text "")]
    (listen webcam-table :selection 
      (fn [e]
        (when-let [row (selection webcam-table)]
          (config! image-label :icon (:CURRENTIMAGEURL (value-at webcam-table row))))))
    (border-panel 
      :id :webcam
      :border 5
      :center (top-bottom-split
                (scrollable webcam-table)
                (scrollable image-label)
                :divider-location 1/2))))

(defn update-webcams [webcam-panel webcams]
  (let [t (select webcam-panel [:#webcam-table])]
    (clear! t)
    (apply insert-at! t (interleave (iterate identity 0) webcams))))

(defn refresh-action-handler [e] 
  (let [root (to-frame e)
        city (text (select root [:#city]))
        forecast-panel (select root [:#forecast])
        webcam-panel (select root [:#webcam])]
    (future 
      (let [forecasts (get-text-forecasts city)
            webcams (get-webcams city)]
        (invoke-later
          (update-webcams webcam-panel webcams)
          (update-forecasts forecast-panel forecasts))))))

(def refresh-action
  (action :name "Refresh" :key "menu R" :handler refresh-action-handler))

(defn make-toolbar []
  (border-panel
    :hgap   5
    :border [5 (line-border :bottom 1 :color divider-color)]
    :west   (label :text "City:" :font title-font)
    :center (text 
              :id   :city
              :text "Ann Arbor,MI" 
              :action refresh-action)))

(defn make-tabs []
  (tabbed-panel
    :tabs [{ :title "Forecast" :content (make-forecast-panel []) }
           { :title "Webcams"  :content (make-webcam-panel) }]))


(defn app []
  (frame
    :title "Gaidica"
    :size  [600 :by 600]
    :on-close :exit
    :menubar (menubar :items [(menu :text "View" :items [refresh-action])])
    :content (border-panel
               :north  (make-toolbar)
               :center (make-tabs))))

(defn -main [& args]
  (reset! api-key (first args))
  (invoke-later (show! (app)))
  ; Avoid RejectedExecutionException in lein :(
  @(promise))

