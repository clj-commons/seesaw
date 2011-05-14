;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.keystroke
  (:use seesaw.keystroke)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [javax.swing KeyStroke]
           [java.awt Toolkit]))

(describe keystroke
  (it "creates a keystroke from a descriptor string"
    (let [ks (keystroke "ctrl S")]
      (expect (= KeyStroke (class ks)))
      (expect (= java.awt.event.KeyEvent/VK_S (.getKeyCode ks))))))

(describe keystroke
  (it "returns nil for nil input"
    (nil? (keystroke nil)))
  (it "returns input if it's a KeyStroke"
    (let [ks (KeyStroke/getKeyStroke "alt X")]
      (expect (= ks (keystroke ks)))))
  (it "returns a keystroke for a string"
    (let [ks (keystroke "alt X")]
      (expect (= java.awt.event.KeyEvent/VK_X (.getKeyCode ks)))))
  (it "substitute platform-specific menu modifier for \"menu\" modifier"
    (let [ks (keystroke "menu X")]
      (expect (= java.awt.event.KeyEvent/VK_X (.getKeyCode ks)))
      (expect (= (.. (Toolkit/getDefaultToolkit) getMenuShortcutKeyMask) (bit-and 7 (.getModifiers ks))))))
  (it "returns a keystroke for a char"
    (let [ks (keystroke \A)]
      (expect (= \A (.getKeyChar ks))))))

