;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns gaidica.core
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            [clojure.contrib.zip-filter.xml :as zfx])
  (:use [seesaw core border]))

(native!)

(def normal-font "ARIAL-12-PLAIN")
(def title-font "ARIAL-14-BOLD")
(def divider-color "#aaaaaa")

(defn get-text-forecasts [xml]
  (for [day  (zfx/xml-> (zip/xml-zip xml):txt_forecast :forecastday)]
    {:title       (zfx/xml1-> day :title zfx/text) 
     :description (zfx/xml1-> day :fcttext zfx/text) 
     :icon        (zfx/xml1-> day :icons :icon_set :icon_url zfx/text)
     :period      (zfx/xml1-> day :period zfx/text)}))

(defn make-forecast-entry [f]
  (mig-panel 
    :constraints ["" "[][grow, fill]" "[top][top]"]
    :border (line-border :bottom 1 :color divider-color)
    :items [
      [(label :icon (:icon f))                   "span 1 2"]
      [(label :text (:title f) :font title-font) "wrap"]
      [(text 
         :opaque? false 
         :multi-line? true 
         :wrap-lines? true 
         :editable? false 
         :text (:description f)
         :font normal-font)                         "wmin 10"]]))

(defn make-forecast-entries [forecasts]
  (map #(vector (make-forecast-entry %) "wrap") forecasts))

(defn make-forecast-panel [forecasts]
  (mig-panel 
    :id :forecast
    :constraints ["" "[grow, fill]"] 
    :items (make-forecast-entries forecasts)))

(defn update-forecasts [forecast-panel xml]
  (let [forecasts (get-text-forecasts xml)]
    (config! forecast-panel :items (make-forecast-entries forecasts))))

(defn make-webcam-panel []
  (top-bottom-split
    (scrollable (table))
    "Under Construction"))


(defn refresh-action-handler [e] 
  (let [root (to-frame e)
        city (text (select root [:#city]))
        forecast-panel (select root [:#forecast])
        url  (str "http://api.wunderground.com/auto/wui/geo/ForecastXML/index.xml?query=" city)]
    (future 
      (let [xml  (xml/parse url)]
        (invoke-later
          (update-forecasts forecast-panel xml))))))

(def refresh-action
  (action :name "Refresh" :key "menu R" :handler refresh-action-handler))

(defn make-toolbar []
  (border-panel
    :hgap   5
    :border [5 (line-border :bottom 1 :color divider-color)]
    :west   (label :text "City:" :font title-font)
    :center (text 
              :id   :city
              :text "Ann%20Arbor,MI" 
              :action refresh-action)))

(defn make-tabs []
  (tabbed-panel
    :tabs [{ :title "Forecast" :content (make-forecast-panel []) }
           { :title "Webcams"  :content (make-webcam-panel) }]))


(defn app []
  (frame
    :title "Gaidica"
    :width  600
    :height 600
    :pack? false
    :on-close :exit
    :menubar (menubar :items [(menu :text "View" :items [refresh-action])])
    :content (border-panel
               :north  (make-toolbar)
               :center (make-tabs))))

(defn -main [& args]
  (invoke-later (app)))

