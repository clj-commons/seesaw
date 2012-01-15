;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for dealing with the mouse."
      :author "Dave Ray"}
  seesaw.mouse
  (:use [seesaw.util :only [illegal-argument]]))

(defn- ^java.awt.PointerInfo info [] (java.awt.MouseInfo/getPointerInfo))

(defn location
  "Returns the [x y] location of the mouse.

  If given no arguments, returns full screen coordinates.

  If given a MouseEvent object returns the mouse location from the event.

  "
  ([] (let [p (.getLocation (info))]
        [(.x p) (.y p)]))
  ([v]
   (cond
     (instance? java.awt.event.MouseEvent v)
        (let [^java.awt.event.MouseEvent e v]
          [(.getX e) (.getY e)])
     :else (illegal-argument "Don't know how to get mouse location from %s" v))))

(def ^ {:private true} input-modifier-table
  {:left java.awt.event.InputEvent/BUTTON1_DOWN_MASK
    :center java.awt.event.InputEvent/BUTTON2_DOWN_MASK
   :right java.awt.event.InputEvent/BUTTON3_DOWN_MASK})

(def ^ {:private true} mouse-button-table
  {java.awt.event.MouseEvent/BUTTON1 :left
   java.awt.event.MouseEvent/BUTTON2 :center
   java.awt.event.MouseEvent/BUTTON3 :right
   java.awt.event.MouseEvent/NOBUTTON nil })

(defn button-down?
  "Returns true if the given button is currently down in the given mouse
  event.

  Examples:

    (button-down? event :left)
  "
  [^java.awt.event.InputEvent e btn]
  (let [mask (input-modifier-table btn 0)]
    (not= 0 (bit-and mask (.getModifiersEx e)))))

(defn button
  "Return the affected button in a mouse event.

  Returns :left, :center, :right, or nil."
  [^java.awt.event.MouseEvent e]
  (mouse-button-table (.getButton e)))

