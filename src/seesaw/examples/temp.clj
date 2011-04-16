(ns seesaw.examples.temp
  (:use seesaw.core seesaw.font))

; See http://stuartsierra.com/2010/01/06/heating-up-clojure-swing 

(defn f-to-c [f]
  (* (- f 32) 5/9))

(defn c-to-f [c]
  (+ (* c 9/5) 32))

(defn parse [s]
  (try (Double/parseDouble (.trim s))
       (catch NumberFormatException e nil)))

(defn display [n]
  (str (Math/round (float n))))

(defn update-temp [source target convert]
  (when (.isFocusOwner source)
    (if-let [n (parse (.getText source))]
      (.setText target (display (convert n)))
      (.setText target ""))))

(defn listen-temp [source target f] 
  (listen source :document (fn [e] (update-temp source target f)))
  source)

(defn temp-app []
  (let [c (text :tip "Enter Celsius temperature") 
        f (text :tip "Enter Fahrenheit temperature")]
    (frame 
      :title "Temp Converter" 
      :content
      (grid-panel 
        :hgap 10 :vgap 10 :columns 2 :border 10
        :items 
        [(label :text "Degrees Celsius" 
                :halign :right 
                :font "ARIAL-BOLD-20"  )
         (listen-temp c f c-to-f)
         (label :text "Degrees Fahrenheit" 
                :halign :right 
                :font {:style :italic :size 20 :name "Arial"})
         (listen-temp f c f-to-c)]))))

(defn -main [& args]
  (invoke-later temp-app))
;(-main) 
