;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.cursor
  (:use seesaw.cursor)
  (:use seesaw.graphics)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [java.awt Cursor]))

(defmacro test-built-ins []
  `(testing "creating a built-in cursor"
    ~@(for [[key value] (dissoc @#'seesaw.cursor/built-in-cursor-map :custom)]
        `(it ~(str "should create a " key " cursor")
          (expect (= ~value (-> (cursor ~key) (.getType))))))))

(describe cursor
  (test-built-ins)
  (it "should return its input if given a cursor"
    (let [c (cursor :hand)]
      (expect (= c (cursor c)))))
  (it "should create a custom cursor from an image with hotspot (0, 0)"
    (let [img (buffered-image 16 16)
          cur (cursor img)]
      ; Can't actually test that the image was set
      (= (Cursor/CUSTOM_CURSOR) (.getType cur))))
  (it "should create a custom cursor from an image with an [x y] hotspot"
    (let [img (buffered-image 16 16)
          cur (cursor img [5 5])]
      ; Can't actually test that the hotspot was set
      (= (Cursor/CUSTOM_CURSOR) (.getType cur))))
  (it "should create a custom cursor from an icon with an [x y] hotspot"
    (let [icon (javax.swing.ImageIcon. (buffered-image 16 16))
          cur (cursor icon [5 5])]
      ; Can't actually test that the hotspot was set
      (= (Cursor/CUSTOM_CURSOR) (.getType cur)))))

