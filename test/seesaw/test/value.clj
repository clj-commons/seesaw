;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.value
  (:use [seesaw.value]
        [seesaw.core])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe value*
  (it "returns a map keyed by id for containers"
    (let [a (label :id :a :text "A")
          b (text  :id :b :text "B")
          p (horizontal-panel :items [a
                                      (vertical-panel :id :foo :items [b]) ])
          f (frame :content p)]
      (expect (= {:a "A" :b "B"}
                 (value* f)))))

  (it "returns the value of a progress bar"
    (= 99 (value* (progress-bar :min 0 :max 100 :value 99))))

  (it "returns the value of a slider"
    (= 99 (value* (slider :min 0 :max 100 :value 99))))

  (it "returns the selection of a spinner"
    (= 101 (value* (selection! (spinner :model [99 100 101 102]) 101))))

  (it "returns the selection of a button-y thing (checkbox, button, menu, etc)"
    (expect (value* (button :selected? true)))
    (expect (nil? (value* (button :selected? false)))))

  (it "returns the selection of a listbox"
    (let [cb (listbox :model ["a" "b" "c"])]
      (selection! cb "b")
      (expect (= "b" (value* cb)))))

  (it "returns the selection of a combobox"
    (let [cb (combobox :model ["a" "b" "c"])]
      (selection! cb "c")
      (expect (= "c" (value* cb)))))

  (it "returns the selection of button-group"
    (let [a (radio)
          b (radio)
          g (button-group :buttons [a b])]
      (selection! g b)
      (expect (= b (value* g)))))

  (it "returns the text of a label"
    (= "bye" (value* (javax.swing.JLabel. "bye"))))

  (it "returns the text of an editor pane"
    (= "bye" (value* (editor-pane :text "bye"))))

  (it "returns the text of styled-text"
    (= "bye" (value* (styled-text :text "bye"))))

  (it "returns the text of a text area"
    (= "bye" (value* (javax.swing.JTextArea. "bye"))))

  (it "returns the text of a text field"
    (= "hi" (value* (javax.swing.JTextField. "hi")))))

(describe value!*
  (it "sets the values of widgets with a map keyed by id for containers"
    (let [a (label :id :a :text "")
          b (text  :id :b :text "")
          c (text :id :c :text "unchanged")
          d (text :id :d :text "something")
          p (horizontal-panel :items [a
                                      (vertical-panel :id :foo :items [b c d]) ])
          f (frame :content p)]
      (expect (= f (value!* f {:a "A" :b "B" :d nil})))
      (expect (= {:a "A" :b "B" :c "unchanged" :d ""} (value* f)))))

  (it "sets the value of a progress-bar"
    (= 99 (-> (progress-bar :min 0 :max 100 :value 98)
            (value!* 99)
            value*)))

  (it "sets the value of a slider"
    (= 99 (-> (slider :min 0 :max 100 :value 98)
            (value!* 99)
            value*)))

  (it "sets the selection of a spinner"
    (= 101 (-> (spinner :model [99 100 101 102])
             (value!* 101)
             value)))

  (it "sets the selection of a button-y thing (checkbox, button, menu, etc)"
    (expect (not (-> (button :selected? true)
              (value!* false)
              value*)))
    (expect (-> (button :selected? false)
              (value!* true)
              value*)))

  (it "sets the selection of a listbox"
    (let [cb (listbox :model ["a" "b" "c"])]
      (expect (nil? (value* cb)))
      (value!* cb "b")
      (expect (= "b" (value* cb)))))

  (it "sets the selection of a combobox"
    (let [cb (combobox :model ["a" "b" "c"])]
      (expect (= "a" (selection cb)))
      (value!* cb "c")
      (expect (= "c" (value* cb)))))
  (it "sets the selection of button-group"
    (let [a (radio)
          b (radio)
          g (button-group :buttons [a b])]
      (expect (nil? (selection g)))
      (value!* g b)
      (expect (= b (value* g)))))
  (it "sets the text of an editor-pane"
    (= "bar" (-> (editor-pane) (value!* "bar") text)))
  (it "sets the text of a styled-text"
    (= "bar" (-> (styled-text) (value!* "bar") text)))
  (it "sets the text of a text area"
    (= "bar" (-> (text :multi-line? true) (value!* "bar") text)))
  (it "sets the text of a text field"
    (= "bar" (-> (text) (value!* "bar") text)))
  (it "sets the text of a text field to \"\" if value is nil"
    (= "" (-> (text "foo") (value!* nil) text)))
  (it "sets the text of a label"
    (= "bar" (-> (label) (value!* "bar") text))))

