;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for creating widget borders."
      :author "Dave Ray"}
  seesaw.border
  (:use seesaw.color)
  (:import [javax.swing BorderFactory]
           [javax.swing.border Border]
           [java.awt Color]))

;*******************************************************************************
; Borders

(declare to-border)

(defn empty-border 
  [& {:keys [thickness top left bottom right]}]
  (if (or top left bottom right)
    (BorderFactory/createEmptyBorder (or top 0) (or left 0) (or bottom 0) (or right 0))
    (let [t (or thickness 1)]
      (BorderFactory/createEmptyBorder t t t t))))

(defn line-border 
  [& {:keys [color thickness top left bottom right] :or {thickness 1 color Color/BLACK}}]
  (if (or top left bottom right)
    (BorderFactory/createMatteBorder 
      (int (or top 0)) (int (or left 0)) (int (or bottom 0)) (int (or right 0)) 
      ^Color (to-color color))
    (BorderFactory/createLineBorder (to-color color) thickness)))

(defn compound-border
  ([b] (to-border b))
  ([b0 b1] (BorderFactory/createCompoundBorder (to-border b1) (to-border b0)))
  ([b0 b1 & more] (reduce #(compound-border %1 %2) (compound-border b0 b1) more)))

(defn to-border 
  ([b] 
    (cond
      (instance? Border b) b
      (integer? b)         (empty-border :thickness b)
      (coll? b)            (apply to-border b)
      true                 (BorderFactory/createTitledBorder (str b))))
  ([b & args]
    (apply compound-border b args)))

