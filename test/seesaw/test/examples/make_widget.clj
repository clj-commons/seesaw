;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.make-widget
  (:use [seesaw core border make-widget mig]
        seesaw.test.examples.example))

; This example shows how to implement the MakeWidget protocol for a new type.
; Is this cute? Yes. Useful? Hell if I know.

; First, let's make a type
(defrecord Person [id first-name last-name])

(defn name-field [person field]
  (text :columns 15 :text (field person)))

; Now implement MakeWidget to create an editor for that type
(extend-type Person
  MakeWidget
  (make-widget* [person]
    (mig-panel :constraints ["", "[][grow]"]
      :border [(line-border :thickness 1) 5]
      :items [
        [ "First Name"                    "gap 10"]
        [ (name-field person :first-name) "growx, wrap"]
        [ "Last Name"                     "gap 10"]
        [ (name-field person :last-name)  "growx"]])))


; Make some people
(def people [
  (Person. 1 "Bob" "Dylan")
  (Person. 2 "James" "Brown")
  (Person. 3 "Lee" "Oswald")
  (Person. 4 "Rita" "Hayward")])

; Pass the people as the :items of a panel and we get a scrollable
; list of widgets.
(defexample []
  (frame :title "People"
    :content (scrollable (vertical-panel :items people))))

;(run :dispose)

