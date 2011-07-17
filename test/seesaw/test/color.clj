;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.color
  (:use seesaw.color)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [java.awt Color]))

(describe get-rgba
  (it "returns vector [r g b a] as integers"
    (= [1 2 3 4] (get-rgba (color 1 2 3 4)))))

(describe color
  (it "can create a color from rgb integers"
    (let [c (color 1 2 3)]
      (expect (= (Color. 1 2 3) c))))
  (it "can create a color from rgba integers"
    (let [c (color 1 2 3 4)]
      (expect (= (Color. 1 2 3 4) c))))
  (it "can create a color from a #-prefixed 6-digit rgb hex string"
    (let [c (color "#010203")]
      (expect (= (Color. 1 2 3) c))))
  (it "can create a color from a #-prefixed 3-digit rgb hex string"
    (let [c (color "#fed")]
      (expect (= (Color. 0xff 0xee 0xdd) c))))
  (it "can create a color from a #-prefixed rgb hex keyword"
    (let [c (color :#010203)]
      (expect (= (Color. 1 2 3) c))))
  (it "can create a color from a #-prefixed rgb hex string and alpha"
    (let [c (color "#010203" 23)]
      (expect (= (Color. 1 2 3 23) c))))
  (it "can create a color from a #-prefixed rgb hex keyword and alpha"
    (let [c (color :#010203 23)]
      (expect (= (Color. 1 2 3 23) c))))
  (it "can create a color from a CSS-style name"
    (expect (= (Color. 240 248 255) (color "aliceblue"))))
  (it "can create a color from a CSS-style keyword name"
    (expect (= (Color. 0 255 127) (color :springgreen))))
  (it "can create a color from a mixed-case CSS-style name"
    (expect (= (Color. 240 248 255) (color "AlIceBlUe")))))

(describe to-color
  (it "returns nil for nil input"
    (nil? (to-color nil)))
  (it "returns its input if its a color"
      (expect (= Color/BLACK (to-color Color/BLACK)))))

(describe default-color
  (it "retrieve a default color from the UIManager"
    (let [name "Label.foreground"
          c (default-color name)
          expected (.getColor (javax.swing.UIManager/getDefaults) name)]
      (expect (not (nil? c)))
      (expect (= c expected)))))


