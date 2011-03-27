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
  (it "can create a color from a #-prefixed rgb hex string"
    (let [c (color "#010203")]
      (expect (= (Color. 1 2 3) c))))
  (it "can create a color from a #-prefixed rgb hex string and alpha"
    (let [c (color "#010203" 23)]
      (expect (= (Color. 1 2 3 23) c)))))

(describe to-color
  (it "returns its input if its a color"
      (expect (= Color/BLACK (to-color Color/BLACK)))))
