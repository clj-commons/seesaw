;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for dealing with widget value. Prefer (seesaw.core/value)."
      :author "Dave Ray"}
  seesaw.value
  (:require [seesaw.selection :as sel]
            [seesaw.selector :as sor]
            [seesaw.util :as util]))

(defprotocol Value
  (container?* [this])
  (value* [this]))

(extend-protocol Value
  java.awt.Container
    (container?* [this] true)
    (value* [this]
      (into {} (->> (sor/select this [:*])
                 (remove container?*)            ; don't recurse
                 (filter #(satisfies? Value %))  ; skip unhandled
                 (map (fn [c]
                        (if-let [id (sor/id-of c)] ; only things with :id
                          [id (value* c)])))
                 (filter identity))))

  javax.swing.JLabel
    (container?* [this] false)
    (value* [this] (.getText this))

  javax.swing.text.JTextComponent
    (container?* [this] false)
    (value* [this] (.getText this))

  javax.swing.JComboBox
    (container?* [this] false)
    (value* [this] (sel/selection this)) 

  javax.swing.JList
    (container?* [this] false)
    (value* [this] (sel/selection this)) 

  javax.swing.AbstractButton
    (container?* [this] false)
    (value* [this] (.isSelected this)) 

  javax.swing.ButtonGroup
    (container?* [this] false)
    (value* [this] (sel/selection this)) 
  
  javax.swing.JSpinner
    (container?* [this] false)
    (value* [this] (sel/selection this)) 

  javax.swing.JSlider
    (container?* [this] false)
    (value* [this] (.getValue this)) 

  javax.swing.JProgressBar
    (container?* [this] false)
    (value* [this] (.getValue this)) 

  ; TODO Tree?
  ; TODO Table?
  )

