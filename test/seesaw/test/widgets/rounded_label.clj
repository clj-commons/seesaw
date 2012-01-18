;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.widgets.rounded-label
  (:use [seesaw.widgets.rounded-label])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe rounded-label
  (it "creates a sub-class of label"
    (instance? javax.swing.JLabel (rounded-label)))
  (it "honors label options"
    (let [rl (rounded-label :text "hi" :background :blue)]
      (expect (= "hi" (.getText rl)))
      (expect (= java.awt.Color/BLUE (.getBackground rl))))))

