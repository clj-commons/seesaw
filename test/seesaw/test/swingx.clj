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

(describe busy-label
  (it "creates a JXBusyLabel"
    (instance? org.jdesktop.swingx.JXBusyLabel (busy-label)))
  (it ":busy? defaults to false"
    (not (core/config (busy-label) :busy?)))
  (it "can set :busy?"
    (core/config (busy-label :busy? true) :busy?))
  (it "can set the text of the label"
    (= "Processing" (core/text (busy-label :text "Processing")))))

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

(describe color-selection-button
  (it "creates a JXColorSelectionButton"
    (instance? org.jdesktop.swingx.JXColorSelectionButton (color-selection-button)))
  (it "can set the initial color"
    (expect (= java.awt.Color/RED 
               (core/config 
                 (color-selection-button :selection java.awt.Color/RED)
                 :selection))))
  (it "can retrieve the current selection with (seesaw.core/selection)"
    (expect (= java.awt.Color/RED 
               (core/selection 
                 (color-selection-button :selection java.awt.Color/RED)))))
  (it "can set the current selection with (seesaw.core/selection!)"
    (let [csb (color-selection-button)]
      (core/selection! csb java.awt.Color/BLACK)
      (expect (= java.awt.Color/BLACK (core/selection csb)))))
  (it "fires :selection event when selection changes"
    (let [called (atom nil)
          csb (color-selection-button :listen [:selection (fn [e] (reset! called e))])]
      (core/selection! csb java.awt.Color/YELLOW)
      (expect @called)
      (expect (= csb (core/to-widget @called)))))
  (it "can remove selection event listener"
    (let [called (atom nil)
          csb (color-selection-button)
          remove-fn (core/listen csb :selection (fn [e] (reset! called e)))]
      (remove-fn)
      (core/selection! csb java.awt.Color/YELLOW)
      (expect (nil? @called)))))

(describe header
  (it "creates a JXHeader"
    (instance? org.jdesktop.swingx.JXHeader (header)))
  (it "can set a title"
    (= "The title" (core/config (header :title "The title") :title)))
  (it "can set a description"
    (= "The description" (core/config (header :description "The description") :description)))
  (it "can set an icon"
    (let [i (icon/icon (graphics/buffered-image 16 16))
          h (header :icon i)]
      (expect (= i (core/config h :icon))))))

(describe xlistbox
  (it "creates a JXList"
    (instance? org.jdesktop.swingx.JXList (xlistbox)))
  (it "creates a JXList with rollover enabled"
    (.isRolloverEnabled (xlistbox)))
  (it "creates a JXList with a default model"
    (let [lb (xlistbox :model [1 2 3])]
      (expect (= 3 (.getSize (core/config lb :model))))))
  (it "takes a comparator to auto-sort the view"
    (let [lb (xlistbox :sort-with < :model [2 1 3 0])]
      (expect (= 3 (.convertIndexToModel lb 0)))))
  (it "does not sort by default"
    (let [lb (xlistbox :model [2 1 3 0])]
      (expect (= 0 (.convertIndexToModel lb 0)))))
  (it "can set the sort order"
    (let [lb (xlistbox :sort-order :ascending)]
      (expect (= javax.swing.SortOrder/ASCENDING (.getSortOrder lb)))
      (core/config! lb :sort-order :descending)
      (expect (= javax.swing.SortOrder/DESCENDING (.getSortOrder lb)))))
  (it "can get the selection when sorted"
    (let [lb (xlistbox :sort-with < :model [2 1 3 0])]
      (core/selection! lb 2)
      (expect (= 2 (core/selection lb))))))

(describe titled-panel
  (it "creates a JXTitledPanel"
    (instance? org.jdesktop.swingx.JXTitledPanel (titled-panel)))
  (it "sets the :title of the panel"
    (= "HI" (.getTitle (titled-panel :title "HI"))))
  (it "sets the :title-color of the panel"
    (= java.awt.Color/RED (.getTitleForeground (titled-panel :title-color :red))))
  (it "sets the :content of the panel"
    (let [c (core/label "HI")
          tp (titled-panel :content c)]
      (expect (= c (.getContentContainer tp)))))
  (it "sets the left and right decorations of the panel"
    (let [left (core/label "HI")
          right (core/button :text "BYE")
          tp (titled-panel :left-decoration left :right-decoration right)]
      (expect (= left (.getLeftDecoration tp)))
      (expect (= right (.getRightDecoration tp))))))

(describe xtree
  (it "creates a JXTree"
    (instance? org.jdesktop.swingx.JXTree (xtree)))
  (it "creates a JXTree with rollover enabled"
    (.isRolloverEnabled (xtree)))
  )

(describe xtable
  (it "creates a JTable"
    (instance? org.jdesktop.swingx.JXTable (xtable)))
  (it "creates a JXTable with rollover enabled"
    (.isRolloverEnabled (xtable)))
  (it "creates a JXTable with column control visible"
    (.isColumnControlVisible (xtable)))
  (it "creates a sortable JXTable"
    (.isSortable (xtable)))
  (it "can show the column control"
    (not (core/config (xtable :column-control-visible? false) :column-control-visible?)))
  (it "can set the column margin"
    (= 99 (core/config (xtable :column-margin 99) :column-margin)))
  )

