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
  (:import [java.awt.datatransfer DataFlavor StringSelection
                                  UnsupportedFlavorException]
           [javax.swing TransferHandler]))

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

(describe default-transferable
  (testing "resulting transferable"
    (it "can hold an arbitrary object"
      (let [o ["hi"]
            t (default-transferable o)]
        (expect (identical? o (.getTransferData t (to-flavor o))))))
    (it "throws UnsupportedFlavorException correctly"
      (let [t (default-transferable "hi")]
        (try (.getTransferData t (to-flavor java.io.File)) false (catch UnsupportedFlavorException e true))))
    (it "implements (getTransferDataFlavors)"
      (let [t (default-transferable [])
            flavors (.getTransferDataFlavors t)]
        (expect (= (to-flavor []) (aget flavors 0)))))
    (it "implements (isDataFlavorSupported)"
      (let [t (default-transferable [])]
        (expect (.isDataFlavorSupported t (to-flavor [])))
        (expect (not (.isDataFlavorSupported t (to-flavor ""))))))))

(defn fake-transfer-support [t]
  (javax.swing.TransferHandler$TransferSupport. (javax.swing.JLabel.) t))

(describe default-transfer-handler
  (it "creates a transfer handler"
    (instance? javax.swing.TransferHandler (default-transfer-handler)))
  (testing "(canImport)"
    (it "returns false if the :import map is missing or empty"
      (not (.canImport (default-transfer-handler) (fake-transfer-support (StringSelection. "hi")))))
    (it "only accepts flavors in the keys of the :import map"
      (let [th (default-transfer-handler :import {String (fn [info])})]
        (expect (.canImport th (fake-transfer-support (StringSelection. "hi"))))
        (expect (not (.canImport th (fake-transfer-support (default-transferable []))))))))
  (testing "(importData)"
    (it "returns false immediately if (canImport) returns false"
      (let [called (atom false)
            th (default-transfer-handler :import {String (fn [info] (reset! called true))})]
        (expect (not (.importData th (fake-transfer-support (default-transferable [])))))
        (expect (not @called))))))

