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

(describe p-built-in
  (it "returns built-in predicates by key"
    (= org.jdesktop.swingx.decorator.HighlightPredicate/ROLLOVER_ROW (p-built-in :rollover-row))))

(describe p-and
  (it "creates an AndHighlightPredicate with the given parts"
    (let [expected-parts [:always :never :even :odd]
          a (apply p-and expected-parts)
          actual-parts (seq (.getHighlightPredicates a))]
      (expect (= actual-parts (map p-built-in expected-parts)))))
  (it "auto-converts regex to pattern predicate"
    (let [pat #"yum"
          p (p-and pat)]
      (expect (= pat (.getPattern (first (.getHighlightPredicates p))))))))

(describe p-or
  (it "creates an OrHighlightPredicate with the given parts"
    (let [expected-parts [:rollover-row :always :even :odd]
          a (apply p-or expected-parts)
          actual-parts (seq (.getHighlightPredicates a))]
      (expect (= actual-parts (map p-built-in expected-parts))))))

(describe p-not
  (it "creates a NotHighlightPredicate with the given target"
    (= (p-built-in :even) (.getHighlightPredicate (p-not :even)))))

(describe p-type
  (it "creates a TypeHighlightPredicate with the given class"
    (= String (.getType (p-type String)))))

(describe p-eq
  (it "creates a EqualsHighlightPredicate with the given value"
    (= "HOWDY" (.getCompareValue (p-eq "HOWDY")))))

(describe p-column-names
  (it "creates a IdentifierHighlighPredicate with the given column ids"
    (= ["a" :b 3] (->> (p-column-names "a" :b 3) .getIdentifiers seq))))

(describe p-column-indexes
  (it "creates a ColumnHighlighPredicate with the given column indexes"
    (= [1 2 4] (->> (p-column-indexes 1 2 4) .getColumns seq))))

(describe p-row-group
  (it "creates a RowGroupHighlightPredicate with the given count"
    (= 6 (.getLinesPerGroup (p-row-group 6)))))

(describe p-depths
  (it "creates a DepthHighlighPredicate with the given depths"
    (= [1 2 4] (->> (p-depths 1 2 4) .getDepths seq))))

(describe p-pattern
  (it "creates a PatternHighlighPredicate with the given pattern"
    (let [pat #"hi"]
      (expect (= pat (.getPattern (p-pattern pat))))))

  (it "creates a PatternHighlighPredicate with the given pattern and columns"
    (let [pat (p-pattern #"hi" :test-column 123 :highlight-column 456)]
      (expect (= 123 (.getTestColumn pat)))
      (expect (= 456 (.getHighlightColumn pat))))))

(describe p-fn
  (it "creates a Highlighter that calls a two-arg function"
    (let [called (atom false)
          p (p-fn (fn [_ _] (reset! called true)))]
      (expect (instance? org.jdesktop.swingx.decorator.HighlightPredicate p))
      (.isHighlighted p nil nil)
      (expect @called))))

(describe hl-color
  (it "returns a function that creates a highlighter with always predicate"
    (let [f (hl-color)
          h1 (f) ]
      (expect (instance? org.jdesktop.swingx.decorator.ColorHighlighter h1))
      (expect (= (p-built-in :always) (.getHighlightPredicate h1)))))

  (it "returns a function that creates a highlight with given predicate"
    (let [f (hl-color)
          h1 (f :never) ]
      (expect (instance? org.jdesktop.swingx.decorator.ColorHighlighter h1))
      (expect (= (p-built-in :never) (.getHighlightPredicate h1)))))
  (it "can control the color of the highlighter"
    (let [f (hl-color :background java.awt.Color/RED
                     :foreground java.awt.Color/ORANGE
                     :selected-background java.awt.Color/GREEN
                     :selected-foreground java.awt.Color/BLUE)
          h (f)]
      (expect (= java.awt.Color/RED (.getBackground h)))
      (expect (= java.awt.Color/ORANGE (.getForeground h)))
      (expect (= java.awt.Color/GREEN (.getSelectedBackground h)))
      (expect (= java.awt.Color/BLUE (.getSelectedForeground h))))))

(describe hl-icon
  (it "returns a function that creates a highlighter with always predicate"
    (let [f (hl-icon nil)
          h1 (f) ]
      (expect (instance? org.jdesktop.swingx.decorator.IconHighlighter h1))
      (expect (= (p-built-in :always) (.getHighlightPredicate h1)))))

  (it "returns a function that creates a highlight with given predicate"
    (let [f (hl-icon nil)
          h1 (f :never) ]
      (expect (instance? org.jdesktop.swingx.decorator.IconHighlighter h1))
      (expect (= (p-built-in :never) (.getHighlightPredicate h1)))))
  (it "can control the icon of the highlighter"
    (let [i (icon/icon (graphics/buffered-image 16 16))
          f (hl-icon i)
          h (f)]
      (expect (= i (.getIcon h))))))

(describe hl-shade
  (it "returns a function that creates a highlighter with always predicate"
    (let [f (hl-shade)
          h1 (f) ]
      (expect (instance? org.jdesktop.swingx.decorator.ShadingColorHighlighter h1))
      (expect (= (p-built-in :always) (.getHighlightPredicate h1)))))

  (it "returns a function that creates a highlight with given predicate"
    (let [f (hl-shade)
          h1 (f :never) ]
      (expect (instance? org.jdesktop.swingx.decorator.ShadingColorHighlighter h1))
      (expect (= (p-built-in :never) (.getHighlightPredicate h1))))))

(describe hl-simple-striping
  (it "returns an simple striping"
    (let [h (hl-simple-striping)]
      (expect (instance? org.jdesktop.swingx.decorator.Highlighter h))))
  (it "returns an simple striping with color"
    (let [h (hl-simple-striping :background :red)]
      (expect (instance? org.jdesktop.swingx.decorator.Highlighter h))))
  (it "returns an simple striping with lines-per-stripe"
    (let [h (hl-simple-striping :lines-per-stripe 6)]
      (expect (instance? org.jdesktop.swingx.decorator.Highlighter h)))))

(defmacro verify-highlighter-host [widget]
  `(let [w# ~widget
         hl# (org.jdesktop.swingx.decorator.HighlighterFactory/createSimpleStriping)]
     (set-highlighters w# [hl#])
     (expect (= [hl#] (get-highlighters w#)))
     (set-highlighters w# [])
     (expect (= nil (get-highlighters w#)))
     (add-highlighter w# hl#)
     (expect (= [hl#] (get-highlighters w#)))
     (remove-highlighter w# hl#)
     (expect (= nil (get-highlighters w#)))
     (core/config! w# :highlighters [hl#])
     (expect (= [hl#] (core/config w# :highlighters)))
     true))

(describe button-x
  (it "creates a JXButton"
    (instance? org.jdesktop.swingx.JXButton (button-x)))
  (it "can set text"
    (= "HI" (core/text (button-x :text "HI"))))
  (it "can set painters"
    (let [p (org.jdesktop.swingx.painter.BusyPainter.)]
      (expect (= p (core/config
                     (button-x :background-painter p)
                     :background-painter)))
      (expect (= p (core/config
                     (button-x :foreground-painter p)
                     :foreground-painter))))))
(describe label-x
  (it "creates a JXLabel"
    (instance? org.jdesktop.swingx.JXLabel (label-x)))
  (it "can set text"
    (= "HI" (core/text (label-x :text "HI"))))
  (it "does not wrap lines by default"
    (not (core/config (label-x :text "HI") :wrap-lines?)))
  (it "can set wrap-lines? option"
    (core/config (label-x :wrap-lines? true) :wrap-lines?))
  (it "can set rotation option"
    (= (Math/toRadians 60.0) (core/config (label-x :text-rotation (Math/toRadians 60.0)) :text-rotation)))
  (it "can set painters"
    (let [p (org.jdesktop.swingx.painter.BusyPainter.)]
      (expect (= p (core/config
                     (label-x :background-painter p)
                     :background-painter)))
      (expect (= p (core/config
                     (label-x :foreground-painter p)
                     :foreground-painter))))))

(describe busy-label
  (it "creates a JXBusyLabel"
    (instance? org.jdesktop.swingx.JXBusyLabel (busy-label)))
  (it ":busy? defaults to false"
    (not (core/config (busy-label) :busy?)))
  (it "can set :busy?"
    (core/config (busy-label :busy? true) :busy?))
  (it "can set the text of the label"
    (= "Processing" (core/text (busy-label :text "Processing")))))

; hyperlink gets grouchy when run on travis with no desktop.
(when (java.awt.Desktop/isDesktopSupported)
  (describe hyperlink
    (it "creates a JXHyperlink with a URI"
      (let [hl (hyperlink :uri (java.net.URI. "http://google.com"))]
        (expect (instance? org.jdesktop.swingx.JXHyperlink hl))))
    (it "creates a JXHyperlink with a string URI"
      (let [hl (hyperlink :uri "http://google.com")]
        (expect (instance? org.jdesktop.swingx.JXHyperlink hl))))))

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

(describe listbox-x
  (it "creates a JXList"
    (instance? org.jdesktop.swingx.JXList (listbox-x)))
  (it "creates a JXList with rollover enabled"
    (.isRolloverEnabled (listbox-x)))
  (it "creates a JXList with a default model"
    (let [lb (listbox-x :model [1 2 3])]
      (expect (= 3 (.getSize (core/config lb :model))))))
  (it "takes a comparator to auto-sort the view"
    (let [lb (listbox-x :sort-with < :model [2 1 3 0])]
      (expect (= 3 (.convertIndexToModel lb 0)))))
  (it "does not sort by default"
    (let [lb (listbox-x :model [2 1 3 0])]
      (expect (= 0 (.convertIndexToModel lb 0)))))
  (it "can set the sort order"
    (let [lb (listbox-x :sort-order :ascending)]
      (expect (= javax.swing.SortOrder/ASCENDING (.getSortOrder lb)))
      (core/config! lb :sort-order :descending)
      (expect (= javax.swing.SortOrder/DESCENDING (.getSortOrder lb)))))
  (it "can get the selection when sorted"
    (let [lb (listbox-x :sort-with < :model [2 1 3 0])]
      (core/selection! lb 2)
      (expect (= 2 (core/selection lb)))))
  (it "is a highlighter host"
    (verify-highlighter-host (listbox-x))))

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
  (it "passes :content through make-widget"
    (let [tp (titled-panel :content "HI")]
      (expect (instance? javax.swing.JLabel (.getContentContainer tp)))))
  (it "sets the left and right decorations of the panel"
    (let [left (core/label "HI")
          right (core/button :text "BYE")
          tp (titled-panel :left-decoration left :right-decoration right)]
      (expect (= left (.getLeftDecoration tp)))
      (expect (= right (.getRightDecoration tp))))))

(describe tree-x
  (it "creates a JXTree"
    (instance? org.jdesktop.swingx.JXTree (tree-x)))
  (it "creates a JXTree with rollover enabled"
    (.isRolloverEnabled (tree-x)))
  (it "is a highlighter host"
    (verify-highlighter-host (tree-x))))

(describe table-x
  (it "creates a JTable"
    (instance? org.jdesktop.swingx.JXTable (table-x)))
  (it "creates a JXTable with rollover enabled"
    (.isRolloverEnabled (table-x)))
  (it "creates a JXTable with column control visible"
    (.isColumnControlVisible (table-x)))
  (it "creates a sortable JXTable"
    (.isSortable (table-x)))
  (it "can enable horizontal scrollbar"
    (core/config (table-x :horizontal-scroll-enabled? true) :horizontal-scroll-enabled?))
  (it "can show the column control"
    (not (core/config (table-x :column-control-visible? false) :column-control-visible?)))
  (it "can set the column margin"
    (= 99 (core/config (table-x :column-margin 99) :column-margin)))
  (it "is a highlighter host"
    (verify-highlighter-host (table-x))))

(describe border-panel-x
  (it "creates a JXPanel with border-panel"
    (expect (instance? java.awt.BorderLayout
                       (.getLayout (border-panel-x :alpha 0.5))))))

(describe flow-panel-x
  (it "creates a JXPanel with flow-panel"
    (expect (instance? java.awt.FlowLayout
                       (.getLayout (flow-panel-x :alpha 0.5))))))

(describe horizontal-panel-x
  (it "creates a JXPanel with horizontal-panel"
    (expect (instance? javax.swing.BoxLayout
                       (.getLayout (horizontal-panel-x :alpha 0.5))))))

(describe vertical-panel-x
  (it "creates a JXPanel with vertical-panel"
    (expect (instance? javax.swing.BoxLayout
                       (.getLayout (vertical-panel-x :alpha 0.5))))))

(describe grid-panel-x
  (it "creates a JXPanel with grid-panel"
    (expect (instance? java.awt.GridLayout
                       (.getLayout (grid-panel-x :alpha 0.5))))))

(describe xyz-panel-x
  (it "creates a JXPanel with xyz-panel"
    (expect (nil? (.getLayout (xyz-panel-x :alpha 0.5))))))

(describe card-panel-x
  (it "creates a JXPanel with card-panel"
    (expect (instance? java.awt.CardLayout
                       (.getLayout (card-panel-x :alpha 0.5))))))
