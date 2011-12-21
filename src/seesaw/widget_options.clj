;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions and protocol for dealing with widget options."
      :author "Dave Ray"}
  seesaw.widget-options
  (:use [seesaw.options :only [OptionProvider get-option-maps*]]))

(defprotocol WidgetOptionProvider
  (get-widget-option-map* [this])
  (get-layout-option-map* [this]))

(extend-protocol OptionProvider
  javax.swing.JComponent 
    (get-option-maps* [this]
      (concat
        (get-widget-option-map* this)
        (get-layout-option-map* this)))

  java.awt.LayoutManager
    (get-option-maps* [this] nil))

(defmacro widget-option-provider [class options & [nil-layout-options]]
  `(extend-protocol WidgetOptionProvider 
     ~class
     (~'get-widget-option-map* [this#] [~options])
     (~'get-layout-option-map* [this#]
      (if-let [layout# (.getLayout this#)]
        (get-option-maps* layout#)
        [~nil-layout-options]))))

