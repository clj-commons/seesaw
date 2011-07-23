;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for dealing with scrolling. Prefer (seesaw.core/scroll!)."
      :author "Dave Ray"}
  seesaw.scroll
  (:use [seesaw.util]))

(defprotocol ^{:private true} ScrollImpl
  (get-handlers [this arg])) 

(def ^{:private true} default-handlers {
  :top    (fn [target] (java.awt.Rectangle. 0 0 0 0))
  :bottom (fn [^java.awt.Component target] 
            (java.awt.Rectangle. 0 (.getHeight target) 0 0))
  :point  (fn [target ^Integer x ^Integer y] (java.awt.Rectangle. x y 0 0))
  :rect   (fn [target ^Integer x ^Integer y ^Integer w ^Integer h] (java.awt.Rectangle. x y w h))
})

(def ^{:private true} list-handlers {
  :row (fn [^javax.swing.JList target ^Integer row]
         (.getCellBounds target row row)) 
})

(def ^{:private true} table-handlers {
  :row    (fn [^javax.swing.JTable target ^Integer row]
            (.getCellRect target row 0 false)) 
  :column (fn [^javax.swing.JTable target ^Integer column]
            (.getCellRect target 0 column false)) 
  :cell   (fn [^javax.swing.JTable target ^Integer row ^Integer column]
            (.getCellRect target row column false)) 
})

(defn- lookup-handler [handlers type]
  (if-let [h (handlers type)] 
    h
    (throw (IllegalArgumentException. (str "Unknown scroll op " type)))))

(defn- ^java.awt.Rectangle to-rect [target v handlers]
  (cond
    (instance? java.awt.Rectangle v) v
    (instance? java.awt.Point v)     (java.awt.Rectangle. ^java.awt.Point v)
    (keyword? v)                     ((lookup-handler handlers v) target)
    (instance? clojure.lang.PersistentVector v)
      (let [[type & args] v]
        (apply (lookup-handler handlers type) target args))))

(defn- scroll-rect-to-visible [^javax.swing.JComponent target rect]
  (when rect
    (.scrollRectToVisible target rect)))

(extend-protocol ScrollImpl
  javax.swing.JComponent (get-handlers [this arg] default-handlers)

  javax.swing.JList
    (get-handlers [this arg] (merge default-handlers list-handlers))

  javax.swing.JTable
    (get-handlers [this arg] (merge default-handlers table-handlers))
  )

(defn scroll!* [target action arg]
  (check-args (not (nil? target)) "target of scroll!* cannot be nil")
  (condp = action
    :to (scroll-rect-to-visible target (to-rect target arg (get-handlers target arg)))))

