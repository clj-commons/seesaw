;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.design-grid
  (:require [seesaw.core :as sc]
            [seesaw.util :as su])
  (:import [net.java.dev.designgridlayout 
              DesignGridLayout DesignGridLayoutManager LabelAlignment]))

; It'd be nice to implement ToWidget on DesignGridLayout
; but it doesn't expose a method to get its component :(

(defn to-design-grid
  [target]
  (cond 
    (instance? DesignGridLayout target) target 
    (instance? DesignGridLayoutManager target) (.designGridLayout target)
    (instance? java.awt.Container target)  (.. target getLayout designGridLayout)))

(def ^{:private true} label-alignments {
  :right LabelAlignment/RIGHT
  :left  LabelAlignment/LEFT
  :platform LabelAlignment/PLATFORM
})

(def ^{:private true} dgp-options {
  :label-alignment #(.labelAlignment (to-design-grid %1) (label-alignments %2))
})

(defn design-grid-panel
  [& opts]
  (let [p (javax.swing.JPanel.)
        l (DesignGridLayout. p)]
    (su/apply-options p opts (merge sc/default-options dgp-options))))

(defn add [p & args]
  (.add p (into-array javax.swing.JComponent (map #(sc/to-widget % true) args))))

(defn row [p] (.row p))
(defn empty-row [p] (.emptyRow p))
(defn center [p] (.center p))
(defn grid 
  ([p w] (.grid p (sc/to-widget w true)))
  ([p w i] (.grid p (sc/to-widget w true) i)))

(defmacro build [target & rows]
  (let [result (gensym "result") 
        dg     (gensym "dg")]
    `(let [~result ~target
           ~dg (seesaw.design-grid/to-design-grid ~result)]
       ~@(map #(concat `(~'-> ~dg) %) rows)
       ~result)))

