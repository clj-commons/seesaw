;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.graphics
  (:use seesaw.graphics)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [java.awt RenderingHints]
           [java.awt.image BufferedImage]))

(describe anti-alias
  (it "turns on anti-aliasing on a graphics object"
    (let [bi (buffered-image 100 100)
          g2d (.getGraphics bi)]
      (anti-alias g2d)
      (expect (= RenderingHints/VALUE_ANTIALIAS_ON (.getRenderingHint g2d RenderingHints/KEY_ANTIALIASING))))))

