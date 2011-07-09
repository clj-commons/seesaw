;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.text-ref
  (:use [seesaw core bind]))

; Very basic example of connecting a text field to an atom.

; A string atom
(def value (atom "initial"))

(defn app []
  (let [input  (text :text @value :columns 20)
        output (text :text @value :editable? false)]
    (bind (.getDocument input) value)
    (bind value (.getDocument output))
    (frame 
      :content 
        (vertical-panel 
          :border 5
          :items ["Enter text here:" 
                  input 
                  :separator
                  "Changed atom is reflected here:"
                  output]))))

(defn -main [& args]
  (invoke-later (show! (pack! (app)))))
;(-main)

