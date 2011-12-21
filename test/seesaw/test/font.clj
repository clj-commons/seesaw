;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.font
  (:use seesaw.font)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [java.awt Font]))

(describe font
  (it "can create a font from a font-spec"
    (let [f (font "ARIAL-BOLD-18")]
      (expect (= "ARIAL" (.getName f)))
      (expect (= 18 (.getSize f))))
      (expect (= Font/BOLD (.getStyle f))))
  (it "can create a bold font"
    (let [f (font :style :bold )]
      (expect (= Font/BOLD (.getStyle f)))))
  (it "can create a bold & italic font"
    (let [f (font :style #{:bold :italic} )]
      (expect (= (bit-or Font/BOLD Font/ITALIC) (.getStyle f)))))
  (it "can create a plain font"
    (let [f (font)]
      (expect (= Font/PLAIN (.getStyle f)))))
  (it "can create an italic font"
    (let [f (font :style :italic)]
      (expect (= Font/ITALIC (.getStyle f)))))
  (it "can create a font with a specific size"
    (let [f (font :size 40)]
      (expect (= 40 (.getSize f)))))
  (it "can create a font from a family keyword"
    (let [f (font :monospaced)]
      (expect (= "Monospaced" (.getFamily f)))))
  (it "can create a monospace font"
    (let [f (font :name :monospaced)]
      (expect (= Font/MONOSPACED (.getName f)))))
  (it "can create a serif font"
    (let [f (font :name :serif)]
      (expect (= Font/SERIF (.getName f)))))
  (it "can create a sans-serif font"
    (let [f (font :name :sans-serif)]
      (expect (= Font/SANS_SERIF (.getName f)))))
  (it "can create a font with a specific typeface"
    (let [f (font :name "Arial")]
      (expect (= "Arial" (.getName f)))))
  (it "can derive a font from another"
    (let [f (font :from (font :name "Arial") :size 33 :style :bold)]
      (expect (= 33 (.getSize f)))
      (expect (= Font/BOLD (.getStyle f)))
      (expect (= "Arial" (.getName f))))))

(describe to-font
  (it "returns nil if its input is nil"
    (nil? (to-font nil)))
  (it "returns its input if its a font"
    (let [f (font)]
      (expect (= f (to-font f)))))
  (it "returns a new font if its input is a font spec"
    (let [f (to-font "ARIAL-ITALIC-14")]
      (expect (= Font/ITALIC (.getStyle f)))))
  (it "returns a new font if its input is a map"
    (let [f (to-font {:style :italic})]
      (expect (= Font/ITALIC (.getStyle f))))))

(describe default-font
  (it "retrieves a named from from the UIManager"
    (let [f (default-font "Label.font")
          expected (.getFont (javax.swing.UIManager/getDefaults) "Label.font")]
      (expect (not (nil? f)))
      (expect (= expected f)))))

