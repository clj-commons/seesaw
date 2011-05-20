;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.design-grid
  (:use [seesaw core design-grid]))

; See http://designgridlayout.java.net/examples.html 

(def names ["Bunny, Bugs",
            "Cat, Sylvester",
            "Coyote, Wile E.",
            "Devil, Tasmanian",
            "Duck, Daffy",
            "Fudd, Elmer",
            "Le Pew, Pepe",
            "Martian, Marvin"])

(defn build-form []
  (build (design-grid-panel :label-alignment :right)
    (row (grid "Last Name") (add (text "Martian")) (grid "First Name") (add (text "Marvin")))
    (row (grid "Phone")     (add (text "805-123-4567")) (grid "Email") (add (text "marvin@wb.com")))
    (row (grid "Address 1") (add (text "1001001010101 Martian Way")))
    (row (grid "Address 2") (add (text "Suite 10111011")))
    (row (grid "City" 1)    (add (text "Ventura")))
    (row (grid "State")     (add (text "CA")) (grid "Postal Code") (add (text "93001")))
    (row (grid "Country" 1) (add (text "USA")))
    (empty-row)
    (row center (add (button :text "New")) (add (button :text "Delete")) (add (button :text "Edit")) (add (button :text "Save")) (add (button :text "Cancel")))))

(defn app []
  (frame 
    :title "DesignGrid Example" 
    :content 
      (border-panel
        :west   (scrollable (listbox :model names))
        :center (build-form))))

(defn -main [& args]
  (invoke-later (app)))
;(-main) 

