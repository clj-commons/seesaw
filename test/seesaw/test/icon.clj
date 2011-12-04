;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.icon
  (:use seesaw.icon)
  (:require [seesaw.graphics :as g]
            [clojure.java.io :as jio])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe icon
  (it "returns nil given nil"
    (nil? (icon nil)))
  (it "returns its input given an Icon"
    (let [i (javax.swing.ImageIcon.)]
      (expect (= i (icon i)))))
  (it "returns an icon given an image"
    (let [image (g/buffered-image 16 16)
          i (icon image)]
      (expect (instance? javax.swing.ImageIcon i))
      (expect (= image (.getImage i)))))
  (it "returns an icon given a URL"
    (let [i (icon (jio/resource "seesaw/test/examples/rss.gif"))]
      (expect (instance? javax.swing.ImageIcon i))))
  (it "returns an icon given a path to an icon on the classpath"
    (let [i (icon "seesaw/test/examples/rss.gif")]
      (expect (instance? javax.swing.ImageIcon i))))
  (it "returns an icon given a File"
    (let [i (icon (java.io.File. "test/seesaw/test/examples/rss.gif"))]
      (expect (instance? javax.swing.ImageIcon i))))
  (it "returns an icon given a i18n keyword"
    (let [i (icon ::test-icon)]
      (expect (instance? javax.swing.ImageIcon i)))))

