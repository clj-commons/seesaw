;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "SwingX integration. Unfortunately, SwingX is hosted on java.net which means
           it looks abandoned most of the time. Downloads are here
           http://java.net/downloads/swingx/releases/1.6/

           This is an incomplete wrapper. If something's missing that you want, just ask."
      :author "Dave Ray"}
  seesaw.swingx
  (:require [seesaw.color])
  (:use [seesaw.util :only [to-uri resource constant-map illegal-argument]]
        [seesaw.icon :only [icon]]
        [seesaw.selection :only [Selection ViewModelIndexConversion]]
        [seesaw.event :only [listen-for-named-event listen-to-property]]
        [seesaw.core :only [construct to-widget make-widget
                            abstract-panel
                            default-options button-options label-options
                            listbox-options tree-options table-options
                            ConfigIcon get-icon* set-icon*
                            config config!]]
        [seesaw.layout :only [default-items-option box-layout grid-layout]]
        [seesaw.options :only [option-map bean-option apply-options
                               default-option resource-option around-option]]
        [seesaw.widget-options :only [widget-option-provider]])
  (:import [org.jdesktop.swingx.decorator
              Highlighter
              HighlighterFactory
              HighlightPredicate
              HighlightPredicate$AndHighlightPredicate
              HighlightPredicate$OrHighlightPredicate
              HighlightPredicate$NotHighlightPredicate
              HighlightPredicate$EqualsHighlightPredicate
              HighlightPredicate$IdentifierHighlightPredicate
              HighlightPredicate$ColumnHighlightPredicate
              HighlightPredicate$RowGroupHighlightPredicate
              HighlightPredicate$DepthHighlightPredicate
              HighlightPredicate$TypeHighlightPredicate]))

;*******************************************************************************
; Highlighter Predicates

(def p-built-in
  (constant-map HighlightPredicate
                :always
                :never
                :even
                :odd
                :integer-negative
                :editable
                :read-only
                :has-focus
                :is-folder
                :is-leaf
                :rollover-row))

(declare p-pattern)
(declare p-fn)

(defn- ^HighlightPredicate to-p [v]
  (cond
    (instance? HighlightPredicate v) v
    (instance? java.util.regex.Pattern v) (p-pattern v)
    (keyword? v) (p-built-in v)
    (fn? v) (p-fn v)
    :else (illegal-argument "Don't know how to make predicate from %s" v)))

(defn p-fn [f]
  (reify
    HighlightPredicate
    (isHighlighted [this renderer adapter]
      (boolean (f renderer adapter)))))

(defn p-and [& args]
  (HighlightPredicate$AndHighlightPredicate.
                      (map to-p args)))

(defn p-or [& args]
  (HighlightPredicate$OrHighlightPredicate.
                      (map to-p args)))

(defn p-not [p]
  (HighlightPredicate$NotHighlightPredicate. (to-p p)))

(defn p-type [class]
  (HighlightPredicate$TypeHighlightPredicate. class))

(defn p-eq [value]
  (HighlightPredicate$EqualsHighlightPredicate. value))

(defn p-column-names [& names]
  (HighlightPredicate$IdentifierHighlightPredicate. (to-array names)))

(defn p-column-indexes [& indexes]
  (HighlightPredicate$ColumnHighlightPredicate. (int-array indexes)))

(defn p-row-group [lines-per-group]
  (HighlightPredicate$RowGroupHighlightPredicate. lines-per-group))

(defn p-depths [& depths]
  (HighlightPredicate$DepthHighlightPredicate. (int-array depths)))

(defn p-pattern [pattern & {:keys [test-column highlight-column]}]
  (org.jdesktop.swingx.decorator.PatternPredicate.
    pattern
    (or test-column -1)
    (or highlight-column -1)))

;*******************************************************************************
; Highlighters

(defn hl-color
  [& {:keys [foreground background
             selected-background selected-foreground]}]
  (fn self
    ([]  (self :always))
    ([p]
      (org.jdesktop.swingx.decorator.ColorHighlighter.
        (to-p p)
        (seesaw.color/to-color background)
        (seesaw.color/to-color foreground)
        (seesaw.color/to-color selected-background)
        (seesaw.color/to-color selected-foreground)))))

(defn hl-icon
  [i]
  (fn self
    ([]  (self :always))
    ([p]
      (org.jdesktop.swingx.decorator.IconHighlighter.
        (to-p p)
        (icon i)))))

(defn hl-shade
  []
  (fn self
    ([]  (self :always))
    ([p]
      (org.jdesktop.swingx.decorator.ShadingColorHighlighter.
        (to-p p)))))

(defn hl-simple-striping
  [& {:keys [background lines-per-stripe]}]
  (cond
    (and background lines-per-stripe)
      (HighlighterFactory/createSimpleStriping
        (seesaw.color/to-color background) lines-per-stripe)
    background
      (HighlighterFactory/createSimpleStriping (seesaw.color/to-color background))
    lines-per-stripe
      (HighlighterFactory/createSimpleStriping lines-per-stripe)
    :else
      (HighlighterFactory/createSimpleStriping)))

(defn ^Highlighter to-highlighter [v]
  (cond
    (instance? Highlighter v) v
    (= :shade v) (hl-shade)
    (= :alternate-striping v) (HighlighterFactory/createAlternateStriping)
    (= :simple-striping v)    (hl-simple-striping)
    :else (illegal-argument "Don't know how to make highlighter from %s" v)))

(defprotocol HighlighterHost
  (get-highlighters* [this])
  (set-highlighters* [this hs])
  (add-highlighter* [this h])
  (remove-highlighter* [this h]))

(defmacro default-highlighter-host
  [class]
  `(extend-protocol HighlighterHost
     ~class
      (~'get-highlighters* [this#]
         (. this# ~'getHighlighters))
      (~'set-highlighters* [this# hs#]
         (. this# ~'setHighlighters hs#))
      (~'add-highlighter* [this# h#]
         (. this# ~'addHighlighter h#))
      (~'remove-highlighter* [this# h#]
         (. this# ~'removeHighlighter h#))))

(defn get-highlighters [target]
  (seq (get-highlighters* (to-widget target))))

(defn set-highlighters [target hs]
  (set-highlighters* (to-widget target)
                     (into-array Highlighter (map to-highlighter hs)))
  target)

(defn add-highlighter [target hl]
  (add-highlighter* (to-widget target) (to-highlighter hl))
  target)

(defn remove-highlighter [target hl]
  (remove-highlighter* (to-widget target) hl)
  target)

(def highlighter-host-options
  (option-map
    (default-option :highlighters set-highlighters get-highlighters)))

;*******************************************************************************
; XButton

(def button-x-options
  (merge
    button-options
    (option-map
      (bean-option :background-painter org.jdesktop.swingx.JXButton)
      (bean-option :foreground-painter org.jdesktop.swingx.JXButton)
      (bean-option :paint-border-insets? org.jdesktop.swingx.JXButton boolean))))

(widget-option-provider org.jdesktop.swingx.JXButton button-x-options)

(defn button-x
  "Creates a org.jdesktop.swingx.JXButton which is an improved (button) that
  supports painters. Supports these additional options:


    :foreground-painter The foreground painter
    :background-painter The background painter
    :paint-border-insets? Default to true. If false painter paints entire
        background.

  Examples:

  See:
    (seesaw.core/button)
    (seesaw.core/button-options)
    (seesaw.swingx/button-x-options)
  "
  [& args]
  (apply-options (construct org.jdesktop.swingx.JXButton) args))

;*******************************************************************************
; XLabel

(def label-x-options
  (merge
    label-options
    (option-map
      ; TODO label-x text-alignment, painter, etc
      (bean-option [:wrap-lines? :line-wrap?] org.jdesktop.swingx.JXLabel boolean)
      (bean-option :text-rotation org.jdesktop.swingx.JXLabel)
      (bean-option :background-painter org.jdesktop.swingx.JXLabel)
      (bean-option :foreground-painter org.jdesktop.swingx.JXLabel))))

(widget-option-provider org.jdesktop.swingx.JXLabel label-x-options)

(defn label-x
  "Creates a org.jdesktop.swingx.JXLabel which is an improved (label) that
  supports wrapped text, rotation, etc. Additional options:

    :wrap-lines? When true, text is wrapped to fit
    :text-rotation Rotation of text in radians

  Examples:

    (label-x :text        \"This is really a very very very very very very long label\"
            :wrap-lines? true
            :rotation    (Math/toRadians 90.0))

  See:
    (seesaw.core/label)
    (seesaw.core/label-options)
    (seesaw.swingx/label-x-options)
  "
  [& args]
  (apply-options (construct org.jdesktop.swingx.JXLabel) args))

;*******************************************************************************
; BusyLabel

(def busy-label-options
  (merge
    label-options
    (option-map
      ; TODO busy-label text-alignment, painter, etc
      (bean-option :busy? org.jdesktop.swingx.JXBusyLabel boolean))))

(widget-option-provider org.jdesktop.swingx.JXBusyLabel busy-label-options)

(defn busy-label
  "Creates a org.jdesktop.swingx.JXBusyLabel which is a label that shows
  'busy' status with a spinner, kind of like an indeterminate progress bar.
  Additional options:

    :busy? Whether busy status should be shown or not. Defaults to false.

  Examples:

    (busy-label :text \"Processing ...\"
                :busy? true)

  See:
    (seesaw.core/label)
    (seesaw.core/label-options)
    (seesaw.swingx/busy-label-options)
  "
  [& args]
  (apply-options (construct org.jdesktop.swingx.JXBusyLabel) args))

;*******************************************************************************
; Hyperlink
(def hyperlink-options
  (merge
    button-options
    (option-map
      (bean-option [:uri :URI] org.jdesktop.swingx.JXHyperlink to-uri))))

(widget-option-provider org.jdesktop.swingx.JXHyperlink hyperlink-options)

(defn hyperlink
  "Constuct an org.jdesktop.swingx.JXHyperlink which is a button that looks like
  a link and opens its URI in the system browser. In addition to all the options of
  a button, supports:

    :uri A string, java.net.URL, or java.net.URI with the URI to open

  Examples:

    (hyperlink :text \"Click Me\" :uri \"http://google.com\")

  See:
    (seesaw.core/button)
    (seesaw.core/button-options)
  "
  [& args]
  (apply-options (construct org.jdesktop.swingx.JXHyperlink) args))

;*******************************************************************************
; TaskPane

(extend-protocol ConfigIcon
  org.jdesktop.swingx.JXTaskPane
    (get-icon* [this] (.getIcon this))
    (set-icon* [this v]
      (.setIcon this (icon v))))

(def task-pane-options
  (merge
    default-options
    (option-map
      default-items-option
      ; TODO I have to add this manually because relying on the impl from default-options
      ; fails with "No implementation of method: :set-icon* :(
      (default-option :icon set-icon* get-icon*)
      (resource-option :resource [:title :icon])
      (bean-option :title org.jdesktop.swingx.JXTaskPane resource)
      (bean-option :animated? org.jdesktop.swingx.JXTaskPane boolean)
      (bean-option :collapsed? org.jdesktop.swingx.JXTaskPane boolean)
      (bean-option :scroll-on-expand? org.jdesktop.swingx.JXTaskPane boolean)
      (bean-option :special? org.jdesktop.swingx.JXTaskPane boolean)
      (default-option :actions
        (fn [^org.jdesktop.swingx.JXTaskPane c actions]
          (doseq [^javax.swing.Action a actions]
            (.add c a)))))))

(widget-option-provider
  org.jdesktop.swingx.JXTaskPane
  task-pane-options)

(defn task-pane
  "Create a org.jdesktop.swingx.JXTaskPane which is a collapsable component with a title
  and icon. It is generally used as an item inside a task-pane-container.  Supports the
  following additional options

    :resource Get icon and title from a resource
    :icon The icon
    :title The pane's title
    :animated? True if collapse is animated
    :collapsed? True if the pane should be collapsed
    :scroll-on-expand? If true, when expanded, it's container will scroll the pane into
                       view
    :special? If true, the pane will be displayed in a 'special' way depending on
              look and feel

  The pane can be populated with the standard :items option, which just takes a
  sequence of widgets. Additionally, the :actions option takes a sequence of
  action objects and makes hyper-links out of them.

  See:
    (seesaw.swingx/task-pane-options)
    (seesaw.swingx/task-pane-container)
  "
  [& args]
  (apply-options
    (construct org.jdesktop.swingx.JXTaskPane)
    args))

(def task-pane-container-options
  (merge
    default-options
    (option-map
      (default-option
        :items
        #(doseq [^org.jdesktop.swingx.JXTaskPane p %2]
           (.add ^org.jdesktop.swingx.JXTaskPaneContainer %1 p))))))

(widget-option-provider
  org.jdesktop.swingx.JXTaskPaneContainer
  task-pane-container-options)

(defn task-pane-container
  "Creates a container for task panes. Supports the following additional
  options:

    :items Sequence of task-panes to display

  Examples:

    (task-pane-container
      :items [(task-pane :title \"First\"
                :actions [(action :name \"HI\")
                          (action :name \"BYE\")])
              (task-pane :title \"Second\"
                :actions [(action :name \"HI\")
                          (action :name \"BYE\")])
              (task-pane :title \"Third\" :special? true :collapsed? true
                :items [(button :text \"YEP\")])])
  See:
    (seesaw.swingx/task-pane-container-options)
    (seesaw.swingx/task-pane)
  "
  [& args]
  (apply-options
    (construct org.jdesktop.swingx.JXTaskPaneContainer)
    args))

;*******************************************************************************
; Color Selection Button

(def color-selection-button-options
  (merge
    button-options
    {:selection (:background button-options)}))

(widget-option-provider
  org.jdesktop.swingx.JXColorSelectionButton
  color-selection-button-options)

(defn color-selection-button
  "Creates a color selection button. In addition to normal button options,
  supports:

    :selection A color value. See (seesaw.color/to-color)

  The currently selected color canbe retrieved with (seesaw.core/selection).

  Examples:

    (def b (color-selection-button :selection :aliceblue))

    (selection! b java.awt.Color/RED)

    (listen b :selection
      (fn [e]
        (println \"Selected color changed to \")))

  See:
    (seesaw.swingx/color-selection-button-options)
    (seesaw.color/color)
  "
  [& args]
  (apply-options
    (construct org.jdesktop.swingx.JXColorSelectionButton)
    args))

; Extend selection and selection event stuff for color button.

(extend-protocol Selection
  org.jdesktop.swingx.JXColorSelectionButton
    (get-selection [this] [(config this :selection)])
    (set-selection [this [v]] (config! this :selection v)))

(defmethod listen-for-named-event
  [org.jdesktop.swingx.JXColorSelectionButton :selection]
  [this event-name event-fn]
  (listen-to-property this "background" event-fn))

;*******************************************************************************
; Header

(extend-protocol ConfigIcon
  org.jdesktop.swingx.JXHeader
    (get-icon* [this]   (.getIcon this))
    (set-icon* [this v] (.setIcon this (icon v))))

(def header-options
  (merge
    default-options
    (option-map
      (bean-option :title org.jdesktop.swingx.JXHeader resource)
      (default-option :icon set-icon* get-icon*)
      (bean-option :description org.jdesktop.swingx.JXHeader resource))))

(widget-option-provider org.jdesktop.swingx.JXHeader header-options)

(defn header
  "Creates a header which consists of a title, description (supports basic HTML)
  and an icon. Additional options:

    :title The title. May be a resource.
    :description The description. Supports basic HTML (3.2). May be a resource.
    :icon The icon. May be a resource.

  Examples:

    (header :title \"This is a title\"
            :description \"<html>A <b>description</b> with some
                          <i>italics</i></html>\"
            :icon \"http://url/to/icon.png\")

  See:
    (seesaw.swingx/header-options)
  "
  [& args]
  (apply-options
    (construct org.jdesktop.swingx.JXHeader)
    args))

;*******************************************************************************
; JXList

(def ^ {:private true} sort-order-table
  { :ascending javax.swing.SortOrder/ASCENDING
    :descending javax.swing.SortOrder/DESCENDING})

; Override view/model index conversion so that the default selection
; handler from JList will work.
(extend-protocol ViewModelIndexConversion
  org.jdesktop.swingx.JXList
    (index-to-model [this index] (.convertIndexToModel this index))
    (index-to-view [this index] (.convertIndexToView this index)))

(default-highlighter-host org.jdesktop.swingx.JXList)

(def listbox-x-options
  (merge
    listbox-options
    highlighter-host-options
    (option-map
      ; When the model is changed, make sure the sort order is preserved
      ; Otherwise, it doesn't look like :sort-with is working.
      (default-option :model
        (fn [c v]
          (let [old (.getSortOrder c)]
            ((:setter (:model listbox-options)) c v)
            (.setSortOrder c old)))
        (:getter (:model listbox-options)))

      (bean-option :sort-order org.jdesktop.swingx.JXList sort-order-table)

      (default-option :sort-with
        (fn [^org.jdesktop.swingx.JXList c v]
          (doto c
            (.setComparator v)
            (.setSortOrder javax.swing.SortOrder/ASCENDING)))
        (fn [^org.jdesktop.swingx.JXList c]
          (.getComparator c))))))

(widget-option-provider org.jdesktop.swingx.JXList listbox-x-options)

(defn listbox-x
  "Create a JXList which is basically an improved (seesaw.core/listbox).
  Additional capabilities include sorting, searching, and highlighting.
  Beyond listbox, has the following additional options:

    :sort-with    A comparator (like <, >, etc) used to sort the items in the
                  model.
    :sort-order   :ascending or descending
    :highlighters A list of highlighters

  By default, ctrl/cmd-F is bound to the search function.

  Examples:

  See:
    (seesaw.core/listbox)
  "
  [& args]
  (apply-options
    (doto (construct org.jdesktop.swingx.JXList)
      (.setAutoCreateRowSorter true)
      (.setRolloverEnabled true))
    args))

;*******************************************************************************
; JXTitledPanel

(def titled-panel-options
  (merge
    default-options
    (option-map
      (resource-option :resource [:title :title-color])
      (bean-option :painter org.jdesktop.swingx.JXTitledPanel)
      (bean-option :title org.jdesktop.swingx.JXTitledPanel resource)
      (bean-option [:title-color :title-foreground] org.jdesktop.swingx.JXTitledPanel seesaw.color/to-color)
      (bean-option [:content :content-container] org.jdesktop.swingx.JXTitledPanel make-widget)
      (bean-option :right-decoration org.jdesktop.swingx.JXTitledPanel make-widget)
      (bean-option :left-decoration org.jdesktop.swingx.JXTitledPanel make-widget))))

(widget-option-provider
  org.jdesktop.swingx.JXTitledPanel
  titled-panel-options)

(defn titled-panel
  "Creates a panel with a title and content. Has the following properties:

    :content The content widget. Passed through (seesaw.core/to-widget)
    :title   The text of the title. May be a resource.
    :title-color Text color. Passed through (seesaw.color/to-color). May
             be resource.
    :left-decoration Decoration widget on left of title.
    :right-decoration Decoration widget on right of title.
    :resource Set :title and :title-color from a resource bundle
    :painter Painter used on the title

  Examples:

    (titled-panel :title \"Error\"
                  :title-color :red
                  :content (label-x :wrap-lines? true
                                   :text \"An error occurred!\"))

  See:
    (seesaw.core/listbox)
  "
  [& args]
  (apply-options
    (construct org.jdesktop.swingx.JXTitledPanel)
    args))

;*******************************************************************************
; JXTree

(default-highlighter-host org.jdesktop.swingx.JXTree)

(def tree-x-options
  (merge
    tree-options
    highlighter-host-options
    (option-map)))

(widget-option-provider org.jdesktop.swingx.JXTree tree-x-options)

(defn tree-x
  "Create a JXTree which is basically an improved (seesaw.core/tree).
  Additional capabilities include searching, and highlighting.
  Beyond tree, has the following additional options:

    :highlighters A list of highlighters

  By default, ctrl/cmd-F is bound to the search function.

  Examples:

  See:
    (seesaw.core/tree-options)
    (seesaw.core/tree)
  "
  [& args]
  (apply-options
    (doto (construct org.jdesktop.swingx.JXTree)
      (.setRolloverEnabled true))
    args))

;*******************************************************************************
; JXTable

(default-highlighter-host org.jdesktop.swingx.JXTable)

(def table-x-options
  (merge
    table-options
    highlighter-host-options
    (option-map
      (bean-option :column-control-visible? org.jdesktop.swingx.JXTable boolean)
      (bean-option :horizontal-scroll-enabled? org.jdesktop.swingx.JXTable boolean)
      (bean-option :column-margin org.jdesktop.swingx.JXTable))))

(widget-option-provider org.jdesktop.swingx.JXTable table-x-options)

(defn table-x
  "Create a JXTable which is basically an improved (seesaw.core/table).
  Additional capabilities include searching, sorting and highlighting.
  Beyond table, has the following additional options:

    :column-control-visible? Show column visibility control in upper right corner.
                             Defaults to true.
    :column-margin           Set margin between cells in pixels
    :highlighters            A list of highlighters
    :horizontal-scroll-enabled? Allow horizontal scrollbars. Defaults to false.

  By default, ctrl/cmd-F is bound to the search function.

  Examples:

  See:
    (seesaw.core/table-options)
    (seesaw.core/table)
  "
  [& args]
  (apply-options
    (doto (construct org.jdesktop.swingx.JXTable)
      (.setRolloverEnabled true)
      (.setColumnControlVisible true))
    args))


;*******************************************************************************
; JXPanel

(def panel-x-options
  (merge
    default-options
    (option-map
      (bean-option :alpha org.jdesktop.swingx.JXPanel))))

(widget-option-provider
  org.jdesktop.swingx.JXPanel
  panel-x-options)

(defn- abstract-panel-x [layout opts]
  (abstract-panel (construct org.jdesktop.swingx.JXPanel) layout opts))

(defn xyz-panel-x [& opts]
  (abstract-panel-x nil opts))

(defn border-panel-x [& opts]
  (abstract-panel-x (java.awt.BorderLayout.) opts))

(defn flow-panel-x [& opts]
  (abstract-panel-x (java.awt.FlowLayout.) opts))

(defn horizontal-panel-x [& opts]
  (abstract-panel-x (box-layout :horizontal) opts))

(defn vertical-panel-x [& opts]
  (abstract-panel-x (box-layout :vertical) opts))

(defn grid-panel-x
  [& {:keys [rows columns] :as opts}]
  (abstract-panel-x (grid-layout rows columns) opts))

(defn card-panel-x [& opts]
  (abstract-panel-x (java.awt.CardLayout.) opts))

