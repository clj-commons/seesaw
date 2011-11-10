;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.swingx
  (:use seesaw.swingx)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe hyperlink
  (it "creates a JXHyperlink with a URI"
    (let [hl (hyperlink :uri (java.net.URI. "http://google.com"))]
      (expect (instance? org.jdesktop.swingx.JXHyperlink hl))))
  (it "creates a JXHyperlink with a string URI"
    (let [hl (hyperlink :uri "http://google.com")]
      (expect (instance? org.jdesktop.swingx.JXHyperlink hl)))))

