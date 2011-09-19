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
  (:use [seesaw.util :only [illegal-argument check-args]]))

(defn- scroll-rect-to-visible [^javax.swing.JComponent target rect]
  (when rect
    (.scrollRectToVisible target rect)))

(def ^{:private true} default-handlers {
  ; TODO preserve current x offset for :top and bottom
  :top
    (fn [target] 
      (scroll-rect-to-visible target (java.awt.Rectangle. 0 0 0 0)))
  :bottom 
    (fn [^java.awt.Component target] 
      (scroll-rect-to-visible target (java.awt.Rectangle. 0 (.getHeight target) 0 0)))
  ; TODO :left and :right
  :point  
    (fn [target ^Integer x ^Integer y] 
      (scroll-rect-to-visible target (java.awt.Rectangle. x y 0 0)))
  :rect   
    (fn [target ^Integer x ^Integer y ^Integer w ^Integer h] 
      (scroll-rect-to-visible target (java.awt.Rectangle. x y w h)))
})

(def ^{:private true} list-handlers {
  :row 
    (fn [^javax.swing.JList target ^Integer row]
      (scroll-rect-to-visible target (.getCellBounds target row row))) 
})

(def ^{:private true} table-handlers {
  ; TODO preserve current column
  :row    
    (fn [^javax.swing.JTable target ^Integer row]
      (scroll-rect-to-visible target (.getCellRect target row 0 false))) 
  ; TODO preserve current row
  :column 
    (fn [^javax.swing.JTable target ^Integer column]
      (scroll-rect-to-visible target (.getCellRect target 0 column false))) 
  :cell   
    (fn [^javax.swing.JTable target ^Integer row ^Integer column]
      (scroll-rect-to-visible target (.getCellRect target row column false))) 
})

(defn- text-position-to-rect [^javax.swing.text.JTextComponent target ^Integer position]
  (try 
    (.modelToView target position)
    (catch javax.swing.text.BadLocationException e nil)))

(defn- text-get-end-position [^javax.swing.text.JTextComponent target]
  (.. target getDocument getEndPosition getOffset))

(defn- set-caret-position [^javax.swing.text.JTextComponent target position]
  (.setCaretPosition target position))

(def ^{:private true} text-handlers {
  ; On text, moving the caret is a better way to scroll. Otherwise, you get
  ; weird behavior because the caret moves off-screen and will cause a jump
  ; as soon as the user tabs to the text component and starts moving the cursor.
  :top
    (fn [^javax.swing.text.JTextComponent target] 
      (set-caret-position target 0))
  :bottom 
    (fn [^java.awt.Component target]
      (set-caret-position target (dec (text-get-end-position target))))
  ; TODO :left and :right
  ; TODO at some point reimplement :point and :rect in terms of caret
  ; position
  :line 
    (fn [^javax.swing.text.JTextComponent target ^Integer line]
      (let [root (.. target getDocument getDefaultRootElement)] 
        (if (and (>= line 0) (< line (.getElementCount root)))
          (set-caret-position target (.. root (getElement line) getStartOffset))))) 

  :position
    (fn [^javax.swing.text.JTextComponent target ^Integer position]
      (if (and (>= position 0) (< position (text-get-end-position target))) 
        (set-caret-position target position)))
})

(defn- lookup-handler [handlers op]
  (if-let [h (handlers op)] 
    h
    (illegal-argument "Unknown scroll op %s" op)))

(defn- canoncicalize-arg 
  "Take the arg to (scroll!*) and turn it into a vector of the form [op & args]"
  [arg]
  (cond
    (instance? java.awt.Rectangle arg) 
      (let [^java.awt.Rectangle r arg] 
        [:rect (.x r) (.y r) (.width r) (.height r)])
    (instance? java.awt.Point arg)     
      (let [^java.awt.Point p arg] 
        [:point (.x p) (.y p)])
    (keyword? arg)                     [arg]
    (vector? arg)                      arg
    :else                              (illegal-argument "Unknown scroll arg format %s" arg))) 

(defprotocol ^{:private true} Scroll
  (scroll-to [this arg])) 

(defn- default-scroll-to-impl [target arg handlers]
  (let [[op & args] arg]
    (apply (lookup-handler handlers op) target args)))

(extend-protocol Scroll
  javax.swing.JComponent 
    (scroll-to [this arg] 
      (default-scroll-to-impl this arg default-handlers))

  javax.swing.JList
    (scroll-to [this arg] 
      (default-scroll-to-impl this arg (merge default-handlers list-handlers)))

  javax.swing.JTable
    (scroll-to [this arg] 
      (default-scroll-to-impl this arg (merge default-handlers table-handlers)))

  javax.swing.text.JTextComponent
    (scroll-to [this arg] 
      (default-scroll-to-impl this arg (merge default-handlers text-handlers)))
 
  ; TODO implement JTree ops
  )

(defn scroll!* [target modifier arg]
  (check-args (not (nil? target)) "target of scroll!* cannot be nil")
  (condp = modifier 
    :to (scroll-to target (canoncicalize-arg arg)))
  target)

