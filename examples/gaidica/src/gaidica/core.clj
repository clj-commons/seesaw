;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns gaidica.core
  (:require [ororo.core :as storm]
            [seesaw.bind :as bind]
            [clojure.string :as string])
  (:use [seesaw core border table mig]))

(native!)

;; Data retrieval stuff -----------------------------------

(def api-key (atom ""))

(defn get-text-forecasts [city]
  (get-in (storm/forecast @api-key city) [:txt_forecast :forecastday]))

(defn get-webcams [city] (storm/webcams @api-key city))

(defn get-rad-sat [city] 
  { :radar     (storm/radar @api-key city)
    :satellite (storm/satellite @api-key city)}) 

;; Widget construction stuff -----------------------------------
;; No behavior here, just building widgets

(def normal-font "ARIAL-12-PLAIN")
(def title-font "ARIAL-14-BOLD")
(def divider-color "#aaaaaa")

(defn make-forecast-entry [f]
  (mig-panel 
    :constraints ["" "[][grow, fill]" "[top][top]"]
    :border (line-border :bottom 1 :color divider-color)
    :items [
      [(label :icon (:icon_url f))               "span 1 2"]
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
  (border-panel 
    :id :webcam
    :border 5
    :center (top-bottom-split
              (scrollable (make-webcam-table))
              (scrollable (label :id :webcam-image :text ""))
              :divider-location 1/2)))

(defn make-radar-panel []
  (border-panel
    :id :radar
    :border 5
    :center 
      (scrollable 
        (grid-panel :columns 2 
          :items [(label :id :radar-image :text "")
                  (label :id :sat-image :text "")
                  (label :id :sat-ir4-image :text "")
                  (label :id :sat-vis-image :text "")]))))

(defn make-toolbar []
  (border-panel
    :hgap   5
    :border [5 (line-border :bottom 1 :color divider-color)]
    :west   (label :text "City:" :font title-font)
    :center (text 
              :id   :city
              :class :refresh
              :text "Ann Arbor,MI")))

(defn make-tabs []
  (tabbed-panel
    :tabs [{ :title "Forecast" :content (make-forecast-panel []) }
           { :title "Webcams"  :content (make-webcam-panel) }
           { :title "Radar/Satellite" :content (make-radar-panel) }]))


(defn make-frame 
  []
  (frame
    :title "Gaidica"
    :size  [600 :by 600]
    :on-close :exit
    :menubar (menubar :items [(menu :text "View" :items [(menu-item :class :refresh)])])
    :content (border-panel
               :border 5
               :hgap 5
               :vgap 5
               :north  (make-toolbar)
               :center (make-tabs)
               :south (label :id :status :text "Ready"))))

;; Behavior stuff -----------------------------------
;; Use selectors and friends to hook behaviors to the widgets built above.

(def panel-behaviors
  [{ :name      "Forecast"
     :data-fn   get-text-forecasts
     :update-fn (fn [root forecasts] 
                  (config! (select root [:#forecast]) 
                           :items (make-forecast-entries forecasts))) }

   { :name      "Webcams"
     :data-fn   get-webcams
     :update-fn (fn [root webcams]
                  (let [t (select root [:#webcam-table])]
                    (clear! t)
                    (apply insert-at! t (interleave (iterate identity 0) webcams)))) }
   
   { :name      "Radar/Satellite"
     :data-fn   get-rad-sat 
     :update-fn (fn [root {:keys [radar satellite]}]
                  (config! (select root [:#radar-image]) :icon (:image_url radar))
                  (config! (select root [:#sat-image]) :icon (:image_url satellite)) 
                  (config! (select root [:#sat-ir4-image]) :icon (:image_url_ir4 satellite)) 
                  (config! (select root [:#sat-vis-image]) :icon (:image_url_vis satellite))) 
    }])

(def active-requests (atom #{}))

; schedule data requests for each panel (:data-fn) and call UI update functions
; (:update-fn) with results, in the UI thread!
(defn refresh-action-handler [e] 
  (let [root (to-frame e)
        city (text (select root [:#city]))]
    (doseq [{:keys [name data-fn update-fn]} panel-behaviors]
      (future 
        (swap! active-requests conj name)
        (let [data (data-fn city)]
          (swap! active-requests disj name)
          (invoke-later
            (update-fn root data)))))))

(def refresh-action
  (action :name "Refresh" :key "menu R" :handler refresh-action-handler))

(defn add-behaviors 
  [root]
  ; As active requests change, update the status bar
  (bind/bind
    active-requests
    (bind/transform #(if (empty? %) "Ready" (str "Refreshing: " (string/join ", " %))))
    (bind/property (select root [:#status]) :text))

  ; Use binding to map selection changes in the table to update the
  ; displayed image
  (bind/bind
    (bind/selection (select root [:#webcam-table]))
    (bind/transform 
      #(-> (select root [:#webcam-table])
         (value-at %) 
         :CURRENTIMAGEURL))
    (bind/property (select root [:#webcam-image]) :icon))
  
  ; Use refresh-action as the action for everything marked with class
  ; :refresh.
  (config! (select root [:.refresh]) :action refresh-action)

  root)

;; Behavior stuff -----------------------------------

(defn -main [& args]
  (when-not (first args)
    (println "Usage: gaidica <wunderground-api-key>")
    (System/exit 1))
  (reset! api-key (first args))
  (invoke-later 
    (-> 
      (make-frame)
      add-behaviors
      show!))
  ; Avoid RejectedExecutionException in lein :(
  @(promise))

