;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.options
  (:require [j18n.core :as j18n])
  (:use seesaw.options
        [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe apply-options
  (it "throws IllegalArgumentException if properties aren't even"
    (try
      (do (apply-options (javax.swing.JPanel.) [1 2 3]) false)
      (catch IllegalArgumentException e true)))
  (it "throws IllegalArgumentException for an unknown property"
    (try
      (do (apply-options (javax.swing.JPanel.) [:unknown "unknown"]) false)
      (catch IllegalArgumentException e true)))
  (it "throws IllegalArgumentException for a property with no setter"
    (try
      (do 
        (apply-options (javax.swing.JPanel.) 
                       [:no-setter "no-setter"]) false)
      (catch IllegalArgumentException e true))))

(describe get-option-value
  (it "throws IllegalArgumentException if target has no handler map"
    (try
      (get-option-value (javax.swing.JPanel.) :text) false
      (catch IllegalArgumentException e true)))
  (it "throws IllegalArgumentException if option doesn't support getter"
    (try
      (get-option-value (javax.swing.JPanel.) :text [{:text (default-option :text nil nil)}]) false
      (catch IllegalArgumentException e true)))
  (it "uses the getter of an option to retrieve a value"
    (= "hi" (get-option-value 
              (javax.swing.JPanel.) 
              :text 
              [{:text (default-option :text nil (constantly "hi"))}]))))

;(describe resource-option
  ;(it "has a setter that applies options using values from resource bundle"
    ;(let [l  (apply-options (javax.swing.JLabel.) 
                          ;[:resource ::resource-option] 
                          ;{:resource (resource-option :resource [:text :name])
                            ;:text (bean-option :text javax.swing.JLabel)
                            ;:name (bean-option :name javax.swing.JLabel) })]
      ;(expect (= "expected text" (.getText l)))
      ;(expect (= "expected name" (.getName l))))))

(describe around-option
  (it "calls the provided converter after calling the getter from the wrapped option"
    (= 100 (get-option-value nil 
                             :foo 
                             [{:foo (around-option 
                                     (default-option :foo identity (constantly 99))
                                     identity 
                                     inc)}])))
  (it "calls the provided converter before calling the setter of the wrapped option"
    (let [result (atom nil)]
      (set-option-value nil 
                        :bar 
                        100
                        [{:bar (around-option
                                (default-option :foo #(reset! result %2))
                                inc
                                identity)}])
      (expect (= 101 @result)))))

