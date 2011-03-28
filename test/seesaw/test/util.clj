(ns seesaw.test.util
  (:use seesaw.util)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

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

