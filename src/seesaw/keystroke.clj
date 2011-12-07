;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.keystroke
  (:use [seesaw.util :only [illegal-argument resource resource-key?]])
  (:import [javax.swing KeyStroke]
           [java.awt Toolkit]
           [java.awt.event InputEvent])
  (:require [clojure.string :only [join split]]))

(def ^{:private true} modifier-masks {
  InputEvent/CTRL_MASK "ctrl"
  InputEvent/META_MASK "meta"
  InputEvent/ALT_MASK  "alt"
})

(defn- preprocess-descriptor [s]
  (let [mask (modifier-masks (.. (Toolkit/getDefaultToolkit) getMenuShortcutKeyMask))]
    (clojure.string/join mask (clojure.string/split s #"menu"))))

(defn keystroke
  "Convert an argument to a KeyStroke. When the argument is a string, follows 
  the keystroke descriptor syntax for KeyStroke/getKeyStroke (see link below).

  For example,

    (keystroke \"ctrl S\")

  Note that there is one additional modifier supported, \"menu\" which will
  replace the modifier with the appropriate platform-specific modifier key for
  menus. For example, on Windows it will be \"ctrl\", while on OSX, it will be
  the \"command\" key. Yay!

  arg can also be an i18n resource keyword.

  See http://download.oracle.com/javase/6/docs/api/javax/swing/KeyStroke.html#getKeyStroke(java.lang.String)"
  [arg]
  (cond 
    (nil? arg)                nil
    (instance? KeyStroke arg) arg
    (char? arg)               (KeyStroke/getKeyStroke ^Character arg)
    (resource-key? arg)       (keystroke (resource arg))
    :else (if-let [ks (KeyStroke/getKeyStroke ^String (preprocess-descriptor (str arg)))]
            ks
            (illegal-argument "Invalid keystroke descriptor: %s" arg))))

