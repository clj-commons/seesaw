;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.meta
  (:use seesaw.meta)
  (:use seesaw.action)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe get-meta
  (testing "when called on a JComponent"
    (it "returns nil if the key is not found"
      (nil? (get-meta (javax.swing.JLabel.) :unknown-key))))
  (testing "when called on an Action"
    (it "returns nil if the key is not found"
      (nil? (get-meta (action) :unknown-key))))
  (testing "when called on an arbitrary object"
    (it "returns nil if the key is not found"
      (nil? (get-meta (javax.swing.JFrame.) :unknown-key)))))

(describe put-meta!
  (testing "when called on a JComponent"
    (it "stores metadata in the component's client properties"
      (let [c (javax.swing.JLabel.)
            result (put-meta! c :some-key 100)]
        (expect (= c result))
        (expect (= 100 (.getClientProperty c :some-key))))))
  (testing "when called on an Action"
    (it "stores metadata in the actions property map"
      (let [a (action)
            result (put-meta! a :some-key 100)]
        (expect (= a result))
        (expect (= 100 (.getValue a (str :some-key)))))))
  (testing "when called on Object"
    (it "stores metadata somewhere, retrievable by get-meta"
      (let [f (javax.swing.JFrame.)
            result (put-meta! f :some-key 10000)]
        (expect (= f result))
        (expect (= 10000 (get-meta f :some-key)))))))

