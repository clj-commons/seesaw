;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.make-widget
  (:use [seesaw.icon :only [icon]])
  (:import [java.awt Dimension]
           [javax.swing Box JLabel JButton]))

(defprotocol MakeWidget
  (make-widget* [v]))

(defmacro ^{:private true} def-make-widget [t b & forms]
  `(extend-type 
     ~t
     MakeWidget 
     (~'make-widget* ~b ~@forms)))

(def-make-widget java.awt.Component [c] c)

(def-make-widget java.awt.Dimension [v] (Box/createRigidArea v))

(def-make-widget javax.swing.Action [v] (JButton. v))

(def-make-widget clojure.lang.Keyword 
  [v] 
  (condp = v
    :separator (javax.swing.JSeparator.)
    :fill-h (Box/createHorizontalGlue)
    :fill-v (Box/createVerticalGlue)))

(def-make-widget clojure.lang.IPersistentVector 
  [[v0 v1 v2]]
  (cond
    (= :fill-h v0) (Box/createHorizontalStrut v1)
    (= :fill-v v0) (Box/createVerticalStrut v1)
    (= :by v1) (Box/createRigidArea (Dimension. v0 v2))))

(def-make-widget String 
  [v]
  (JLabel. v))

(def-make-widget java.net.URL
  [v]
  (JLabel. (icon v)))

