;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.cells
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]
        [seesaw cells font]))

(describe default-list-cell-renderer
  (it "proxies a DefaultListCellRenderer which dispatches to functions in a map"
    (let [expected-font (font :name "ARIAL-BOLD-18")
          jlist (javax.swing.JList.)
          r (default-list-cell-renderer :foreground (fn [info] java.awt.Color/YELLOW)
                                        :text (fn [info] "hi")
                                        :icon (fn [info] nil)
                                        :font (fn [info] expected-font))
          c (.getListCellRendererComponent r jlist nil 0 false false)]
      (expect (= java.awt.Color/YELLOW (.getForeground c)))
      (expect (= "hi" (.getText c)))
      (expect (= nil (.getIcon c)))
      (expect (= expected-font (.getFont c))))))

