;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.swingx
  (:require [seesaw.core :as core])
  (:require [seesaw.icon :as icon])
  (:require [seesaw.graphics :as graphics])
  (:use seesaw.swingx)
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe xlabel
  (it "creates a JXLabel"
    (instance? org.jdesktop.swingx.JXLabel (xlabel)))
  (it "can set text"
    (= "HI" (core/text (xlabel :text "HI"))))
  (it "does not wrap lines by default"
    (not (core/config (xlabel :text "HI") :wrap-lines?)))
  (it "can set wrap-lines? option"
    (core/config (xlabel :wrap-lines? true) :wrap-lines?))
  (it "can set rotation option"
    (= (Math/toRadians 60.0) (core/config (xlabel :text-rotation (Math/toRadians 60.0)) :text-rotation))))

(describe hyperlink
  (it "creates a JXHyperlink with a URI"
    (let [hl (hyperlink :uri (java.net.URI. "http://google.com"))]
      (expect (instance? org.jdesktop.swingx.JXHyperlink hl))))
  (it "creates a JXHyperlink with a string URI"
    (let [hl (hyperlink :uri "http://google.com")]
      (expect (instance? org.jdesktop.swingx.JXHyperlink hl)))))

(describe task-pane
  (it "creates a JXTaskPane with a title and icon"
    (let [i (icon/icon (graphics/buffered-image 16 16))
          tp (task-pane :title "HI" :icon i)]
      (expect (instance? org.jdesktop.swingx.JXTaskPane tp))
      (expect (= "HI" (core/config tp :title)))
      (expect (= i (core/config tp :icon)))))
  (it "create a JXTaskPane with actions"
    (let [a  (core/action :name "A")
          b  (core/action :name "B")
          tp (task-pane :actions [a b] )]
      (expect (= 2 (.getComponentCount (.getContentPane tp)))))))

(describe task-pane-container
  (it "creates a JXTaskPaneContainer with some items"
    (let [tpc (task-pane-container)]
      (expect (instance? org.jdesktop.swingx.JXTaskPaneContainer tpc)))))

