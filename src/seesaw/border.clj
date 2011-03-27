(ns seesaw.border
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
    (BorderFactory/createMatteBorder (or top 0) (or left 0) (or bottom 0) (or right 0) (to-color color))
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

