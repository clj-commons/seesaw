;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.dnd
  (:use seesaw.dnd)
  (:use [seesaw.graphics]
        [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [java.awt.datatransfer DataFlavor]))

(describe to-flavor
  (it "returns the flavor if it's already a flavor"
    (= DataFlavor/stringFlavor (to-flavor DataFlavor/stringFlavor)))

  (it "creates a JVM local flavor for an arbitrary class"
    (let [c (class [])
          f (to-flavor c)]
      (expect (= (format "%s; class=%s" DataFlavor/javaJVMLocalObjectMimeType (.getName c)) (.getMimeType f)))))

  (it "creates a JVM local flavor for an arbitrary value"
    (= (to-flavor (class [])) (to-flavor [])))
  (it "returns a string flavor for a String class"
    (= DataFlavor/stringFlavor (to-flavor String)))
  (it "returns a string flavor for a string value"
    (= DataFlavor/stringFlavor (to-flavor "hello")))
  (it "returns a file list flavor for a File class"
    (= DataFlavor/javaFileListFlavor (to-flavor java.io.File)))
  (it "returns a file list flavor for a file value"
    (= DataFlavor/javaFileListFlavor (to-flavor (java.io.File. "."))))
  (it "returns an image flavor for an Image class"
    (= DataFlavor/imageFlavor (to-flavor java.awt.Image)))
  (it "returns an image flavor for an image value"
    (= DataFlavor/imageFlavor (to-flavor (buffered-image 10 10)))))

