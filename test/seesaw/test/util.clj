(ns seesaw.test.util
  (:use seesaw.util)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe cond-doto
  (it "only executes forms with true conditions"
    (= "firstsecondfifth" (str (cond-doto (StringBuilder.) 
         true (.append "first") 
         (> 2 1) (.append "second")
         (< 2 1) (.append "third")
         false (.append "fourth")
         (= "HI" "HI") (.append "fifth"))))))

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
  (it "returns a URL if (str input) is a valid URL"
    (= "http://darevay.com" (-> (to-url "http://darevay.com") .toExternalForm )))
  (it "returns nil if (str input is not a valid URL"
    (nil? (to-url "not a URL"))))

