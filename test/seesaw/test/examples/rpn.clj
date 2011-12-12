;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.rpn
  (:use [seesaw.core]
        [seesaw.style :only [apply-stylesheet]]
        seesaw.test.examples.example)
  (:require [seesaw.bind :as bind]))

;; A simple RPN calculator

(defn operator 
  "make a function that takes the stack, pops arity args, applies f to them
  and returns the new stack with the result on top. If f returns nil, does
  nothing."
  [f arity]
  (fn [stack] 
    (let [[args more] (split-at arity stack)]
      (if-let [result (apply f args)] 
        (cons result more)
        more))))

(defn push-op 
  "Return an operator that pushes v"
  [v] (operator (constantly v) 0))

(defn calculate 
  "Start with the given stack and apply each of the given op functions
  to it and return the resulting stack."
  [stack & ops]
  (reduce
    (fn [stack op] (op stack))
    stack
    ops))

; A declarative version of our operators with fn, arity, and display
(def operators 
  { :+    { :op +                :arity 2 :text "+" }
    :-    { :op -                :arity 2 :text "-" }
    :*    { :op *                :arity 2 :text "x" }
    :div  { :op /                :arity 2 :text "/" }
    :sqrt { :op #(Math/sqrt %)   :arity 1 :text "sqrt"}
    :inv  { :op #(/ 1 %)         :arity 1 :text "1 / x"}
    ; Making push a noop here simplifies things below.
    :push { :op (constantly nil) :arity 0 :text "Push"}})

; Define the layout of the UI. 
(defn layout []
  (frame
    :title "RPN Calculator"
    :content 
      (border-panel
        :border 5
        :hgap   5 
        :vgap   5
        :north  (text :id :value)
        :center (grid-panel :columns 3 
                  :items (concat
                           (map #(button :class :digit :text %) [7 8 9 
                                                                 4 5 6 
                                                                 1 2 3 
                                                                 0])
                           [(button :id :point :class :digit :text ".")]))
        :west   (scrollable (listbox :id :stack) :preferred-size [150 :by 0])
        :east   (grid-panel :columns 1
                  :items (map #(button :id (key %) 
                                       :class :operator 
                                       :text (:text (val %))) operators)))))

; Install behaviors on the UI.
(defn behave 
  [root]
  (let [stack         (atom [])               ; Holds the current stack state
        value         (select root [:#value]) ; The value text box
        current-value (atom nil)]             ; The current displayed value as double or nil

    ; As the text box changes, convert to double, or nil for empty.
    (bind/bind 
      value 
      (bind/transform #(if (empty? %) nil (Double/valueOf %))) 
      current-value)

    ; As text box changes, enable/disable decimal point button
    (bind/bind
      value
      (bind/transform #(not (some #{\.} %)))
      (bind/property (select root [:#point]) :enabled?))

    ; The digit buttons just append their number (or decimal point) to the text box
    (listen (select root [:.digit])
            :action (fn [e] 
                      (let [c (text e)] 
                        (text! value (str (text value) c)))))

    ; The operator buttons look up their operator in the table, apply it
    ; to the current stack and clear the text box
    (listen (select root [:.operator])
            :action (fn [e]
                      (let [{:keys [op arity]} (operators (id-of e))] 
                        (swap! stack calculate (push-op @current-value) (operator op arity))
                        (text! value ""))))

    ; Show the stack as it's updated
    (bind/bind stack (bind/property (select root [:#stack]) :model)))
  root)

; Set styles. NOTE this uses an alpha API and will most likely change
(def style 
  { [:#value] {:halign    :right 
               :font      "ARIAL-PLAIN-20" 
               :editable? false}
    [:#stack] {:font "ARIAL-PLAIN-20"} })

(defexample [] 
  (->
    (layout)
    behave
    (apply-stylesheet style) ; <- NOTE use of experimental/alpha function
    ))

;(run :dispose)

