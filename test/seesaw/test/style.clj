;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.style
  (:use seesaw.style
        [seesaw.core :only [border-panel label button config text]]
        [seesaw.color :only [to-color]])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe apply-stylesheet

  (it "returns its input"
    (let [lbl (label)]
      (expect (= lbl (apply-stylesheet lbl {})))))

  (it "changes styles of widget for rules that match"
    (let [lbl (label :id :lbl)
          btn-a (button :class :btn)
          btn-b (button :class :btn :id :btn-b)
          p (border-panel :center lbl :north btn-a :south btn-b)]
      (apply-stylesheet p
        {[:#lbl] { :background :aliceblue
                   :text "hi"}
         [:.btn] { :foreground :red }
         [:#btn-b] {:text "B"}})
      (expect (= (to-color :aliceblue) (config lbl :background)))
      (expect (= "hi" (text lbl)))
      (expect (= "B" (text btn-b)))
      (expect (= (to-color :red) (config btn-a :foreground)))
      (expect (= (to-color :red) (config btn-b :foreground))))))
