;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for dealing with drag and drop and data transfer."
      :author "Dave Ray"}
  seesaw.dnd
  (:import [java.awt.datatransfer DataFlavor]))

(defn ^DataFlavor to-flavor
  [v]
  (cond
    (instance? DataFlavor v) v
    (= v String) DataFlavor/stringFlavor
    (= v java.io.File) DataFlavor/javaFileListFlavor
    (= v java.awt.Image) DataFlavor/imageFlavor
    (instance? java.awt.Image v) DataFlavor/imageFlavor
    (class? v) (DataFlavor. (format "%s; class=%s" DataFlavor/javaJVMLocalObjectMimeType (.getName v)))

    :else (to-flavor (class v))))

