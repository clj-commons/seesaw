;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.text-ref
  (:use [seesaw core]))

; Very basic example of connecting a text field to a ref.

; A string ref
(def value (ref "initial"))

; document event handler that copies text to ref
(defn document-change-handler [e]
  (dosync
    (ref-set value (text e))))

; watch the ref and reflect changes in output component 
(defn watch-value [output]
  (add-watch value nil 
    (fn [_ r old-state new-state] 
      (text! output new-state))))

(defn app []
  (let [input  (text :text @value :columns 20)
        output (text :text @value :editable? false)]
    (listen input :document document-change-handler)
    (watch-value output)
    (frame 
      :content 
        (vertical-panel 
          :border 5
          :items ["Enter text here:" 
                  input 
                  :separator
                  "Changed ref is reflected here:"
                  output]))))

(defn -main [& args]
  (invoke-later (app)))
;(-main)

