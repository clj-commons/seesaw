;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.to-widget
  (:use [seesaw.util :only [try-cast]])
  (:import [java.awt Dimension]
           [javax.swing Box JLabel JButton]))

(defprotocol ToWidget 
  (to-widget* [v]))

(defmacro ^{:private true} def-to-widget [t b & forms]
  `(extend-type 
     ~t
     ToWidget 
      (~'to-widget*   ~b ~@forms)))

(def-to-widget Object [c] nil)

(def-to-widget java.awt.Component [c] c)

(def-to-widget java.util.EventObject 
  [v] 
  (try-cast java.awt.Component (.getSource v)))

