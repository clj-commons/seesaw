;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.mig
  (:use seesaw.mig seesaw.core)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe mig-panel
  (it "should create a panel with a MigLayout"
    (expect (= net.miginfocom.swing.MigLayout (class (.getLayout (mig-panel))))))
  (it "should set MigLayout layout constraints"
    (let [p (mig-panel :constraints ["wrap 4", "[fill]", "[nogrid]"])
          l (.getLayout p)]
      (expect (= "wrap 4" (.getLayoutConstraints l)))
      (expect (= "[fill]" (.getColumnConstraints l)))
      (expect (= "[nogrid]" (.getRowConstraints l))))))

(describe replace!
  (testing "when called on a panel with a mid layout"
    (it "replaces the given widget with a new widget and maintains constraints"
      (let [l0 (label "l0")
            l1 (label "l1")
            l2 (label "l2")
            p (mig-panel :items [[l0 ""] [l1 "wrap"]])
            result (replace! p l1 l2)]
        (expect (= p result))
        (expect (= [l0 l2] (vec (.getComponents p))))
        (expect (= "wrap" (-> p .getLayout (.getComponentConstraints l2))))))))

