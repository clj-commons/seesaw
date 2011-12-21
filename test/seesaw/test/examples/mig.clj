;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.mig
  (:use [seesaw core mig]
        seesaw.test.examples.example))

; http://www.devx.com/Java/Article/38017/1954

(defn frame-content []
  (mig-panel :constraints ["", "[right]"]
    :items [
      [ "General"          "split, span, gaptop 10"]
      [ :separator         "growx, wrap, gaptop 10"]
      [ "Company"          "gap 10"]
      [ (text)             "span, growx"]
      [ "Contact"          "gap 10"]
      [ (text)             "span, growx, wrap"]
                           
      [ "Propeller"        "split, span, gaptop 10"]
      [ :separator         "growx, wrap, gaptop 10"]

      [ "PTI/kW"           "gap 10"]
      [ (text :columns 10) ""]
      [ "Power/kW"         "gap 10"]
      [ (text :columns 10) "wrap"]
      [ "R/mm"             "gap 10"]
      [ (text :columns 10) ""]
      [ "D/mm"             "gap 10"]
      [ (text :columns 10) ""]]))

(defexample []
  (frame :title "MigLayout Example" :resizable? false :content (frame-content)))

;(run :dispose) 

