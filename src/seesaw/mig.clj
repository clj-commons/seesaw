;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "MigLayout support for Seesaw"
      :author "Dave Ray"}
  seesaw.mig
  (:use [seesaw.core :only [abstract-panel default-options]]
        [seesaw.layout :only [LayoutManipulation add-widget handle-structure-change]]
        [seesaw.options :only [default-option option-map option-provider]]
        [seesaw.util :only [cond-doto]]))

;*******************************************************************************
; MigLayout
(defn- apply-mig-constraints [^java.awt.Container widget constraints]
  (let [^net.miginfocom.swing.MigLayout layout (.getLayout widget)
        [lc cc rc] constraints]
    (cond-doto layout
      lc (.setLayoutConstraints lc)
      cc (.setColumnConstraints cc)
      rc (.setRowConstraints rc))))

(defn- add-mig-items [^java.awt.Container parent items]
  (.removeAll parent)
  (doseq [[widget constraint] items]
    (add-widget parent widget constraint))
  (handle-structure-change parent))

(def mig-layout-options
  (option-map
    (default-option :constraints apply-mig-constraints)
    (default-option :items add-mig-items)))

(option-provider net.miginfocom.swing.MigLayout mig-layout-options)

(def mig-panel-options default-options)

(defn mig-panel
  "Construct a panel with a MigLayout. Takes one special property:

      :constraints [\"layout constraints\" \"column constraints\" \"row constraints\"]

  These correspond to the three constructor arguments to MigLayout.
  A vector of 0, 1, 2, or 3 constraints can be given.

  The format of the :items property is a vector of [widget, constraint] pairs.
  For example:

    :items [[ \"Propeller\"        \"split, span, gaptop 10\"]]

  See:
    http://www.miglayout.com
    (seesaw.core/default-options)
  "
  { :seesaw {:class 'javax.swing.JPanel }}
  [& opts]
  (abstract-panel (net.miginfocom.swing.MigLayout.) opts))

(extend-protocol LayoutManipulation
  net.miginfocom.swing.MigLayout
    (add!* [layout target widget constraint]
      (add-widget target widget constraint))
    (get-constraint* [layout container widget]
      (.getComponentConstraints layout widget)))

