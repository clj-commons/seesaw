;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.rsyntax
  (:use [seesaw core]
        seesaw.test.examples.example)
  (:require [seesaw.rsyntax :as rsyntax]
            [clojure.java.io :as io]))

(defn make-frame []
  (frame :title "RSyntax Example" :width 500 :height 400
    :content (scrollable
               (rsyntax/text-area
                 :text (io/resource "seesaw/test/examples/rsyntax.clj")
                 :syntax :clojure))))

(defexample []
  (make-frame) )

;(run :dispose)
