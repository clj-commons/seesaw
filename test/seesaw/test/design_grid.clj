;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.design-grid
  (:use seesaw.core)
  (:require [seesaw.design-grid :as sdg])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe design-grid-panel
  (it "should create a panel with a design grid layout"
    (let [p (sdg/design-grid-panel)]
      (expect (instance? net.java.dev.designgridlayout.DesignGridLayoutManager (.getLayout p))))))

(describe to-design-grid
  (it "should retrieve the DesignGridLayout instance from a panel"
    (let [p (sdg/design-grid-panel)]
      (expect (instance? net.java.dev.designgridlayout.DesignGridLayout (sdg/to-design-grid p))))))

