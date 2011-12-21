;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.temp
  (:use seesaw.core 
        seesaw.font
        seesaw.test.examples.example))

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
    (if-let [n (parse (text source))]
      (text! target (display (convert n)))
      (text! target ""))))

(defn listen-temp [source target f] 
  (listen source :document (fn [e] (update-temp source target f)))
  source)

(defexample []
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

;(run :dispose) 

