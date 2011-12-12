;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.text-ref
  (:use [seesaw.core]
        seesaw.test.examples.example)
  (:require [seesaw.bind :as bind]))

; Very basic example of connecting a text field to an atom.

(defexample []
  (let [input  (text :columns 20)
        value  (atom "")
        output (text :editable? false)]
    ; Set up our binding chain
    (bind/bind input                         ; Take changes to input
          (bind/transform #(.toUpperCase %)) ; Pass through upper case transform
          value                         ; Put the value in the atom 
          output)                       ; Show the final value in the output text doc
    (text! input "Initial Value")
    (frame 
      :content 
        (vertical-panel 
          :border 5
          :items ["Enter text here:" 
                  input 
                  :separator
                  "Changed atom is reflected here:"
                  output
                  :separator
                  (action :name "Print atom value to console"
                          :handler (fn [e] (println "Current atom value is: " @value)))]))))

;(run :dispose)

