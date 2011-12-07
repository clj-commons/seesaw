;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.util
  (:use seesaw.util)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe illegal-argument
  (it "throws a formatted illegal argument exception"
    (try
      (illegal-argument "This %s a message with code %d" "is" 99) false
      (catch IllegalArgumentException e
        (= "This is a message with code 99" (.getMessage e))))))

(describe check-args
  (it "returns true if the condition is true"
    (check-args true "yes!"))
  (it "returns throws IllegalArgumentException if condition is false"
    (try 
      (do (check-args false "no!") false)
      (catch IllegalArgumentException e true))))

(describe cond-doto
  (it "only executes forms with true conditions"
    (= "firstsecondfifth" (str (cond-doto (StringBuilder.) 
         true (.append "first") 
         (> 2 1) (.append "second")
         (< 2 1) (.append "third")
         false (.append "fourth")
         (= "HI" "HI") (.append "fifth"))))))

(describe to-seq
  (it "makes a non-seq into a single-element seq"
    (= (seq ["hi"]) (to-seq "hi"))
    (= (seq [:k]) (to-seq :k)))
  (it "makes a collection into a seq"
    (= (seq #{:a :b}) (to-seq #{:a :b}))))


(describe camelize
  (it "turns dashes into camel humps"
    (= "onMouseClicked" (camelize "on-mouse-clicked"))))

(describe boolean?
  (it "returns true for true"
    (boolean? true))
  (it "returns true for false"
    (boolean? false))
  (it "returns false for nil"
    (not (boolean? nil)))
  (it "returns false for non-boolean"
    (not (boolean? "hi"))))

(describe try-cast
  (it "returns its input if cast succeeds"
    (= "TEST" (try-cast java.lang.String "TEST")))
  (it "returns nil if input is nil"
    (nil? (try-cast java.lang.String nil)))
  (it "returns nil if cast fails"
    (nil? (try-cast java.lang.String 99))))

(describe to-url
  (it "returns its input if it is already a URL object"
    (let [u (java.net.URL. "http://google.com")]
      (expect (identical? u (to-url u)))))
  (it "returns a URL if (str input) is a valid URL"
    (= "http://darevay.com" (-> (to-url "http://darevay.com") .toExternalForm )))
  (it "returns nil if (str input) is not a valid URL"
    (nil? (to-url "not a URL"))))

(describe to-uri
  (it "returns its input if it is already a URI object"
    (let [u (java.net.URI. "http://google.com")]
      (expect (identical? u (to-uri u)))))
  (it "returns a URI if (str input) is a valid URI"
    (= "http://darevay.com" (-> (to-uri "http://darevay.com") .toString )))
  (it "returns nil if (str input) is not a valid URI"
    (nil? (to-url "not a URI"))))

(describe to-dimension
  (it "should throw an exception if it doesn't know what to do"
    (try
      (do (to-dimension {:a :map}) false)
      (catch IllegalArgumentException e true)))
  (it "should return its input if its already a Dimension"
    (let [d (java.awt.Dimension. 10 20)]
      (expect (= d (to-dimension d)))))
  (it "should return a new Dimension if input is [width :by height]"
    (let [d (to-dimension [1 :by 2])]
      (expect (= java.awt.Dimension (class d)))
      (expect (= 1 (.width d)))
      (expect (= 2 (.height d))))))

(describe to-insets
  (it "should throw an exception if it doesn't know what to do"
    (try
      (do (to-insets "a random string") false)
      (catch IllegalArgumentException e true)))
  (it "should return its input if its already an Insets"
    (let [i (java.awt.Insets. 1 2 3 4)]
      (expect (= i (to-insets i)))))
  (it "should return uniform insets from a number"
    (= (java.awt.Insets. 9 9 9 9) (to-insets 9)))
  (it "should return insets from a 4-element [top, left, bottom, right] vector"
    (= (java.awt.Insets. 1 2 3 4) (to-insets [1 2 3 4])))
  (it "should return insets from a 2-element [top/bottom, left/right] vector"
    (= (java.awt.Insets. 5 6 5 6) (to-insets [5 6]))))

(describe atom?
  (it "should return true for an atom"
    (atom? (atom nil)))
  (it "should return false for a non-atom"
    (not (atom? (ref nil)))))

(describe to-mnemonic-keycode
  (it "should pass through an integer key code"
    (= 99 (to-mnemonic-keycode 99)))
  (it "should convert a character to an integer key code"
    (= (int \T) (to-mnemonic-keycode \T)))
  (it "should convert a lower-case character to an integer key code"
    (= (int \X) (to-mnemonic-keycode \x))))

(describe resource-key?
  (it "returns true for resource keywords"
    (resource-key? ::hello))
  (it "returns false for non-resource keywords"
    (not (resource-key? :hello))))

