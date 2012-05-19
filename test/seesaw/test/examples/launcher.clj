;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.launcher
  (:use [seesaw core swingx keymap]
        seesaw.test.examples.example)
  (:require [seesaw.bind :as b]
            [seesaw.dev :as dev]))

; A simple launcher for all the examples.

(def examples
  ['behave
   'bind
   'button-group
   'canvas
   'cell-renderers
   'clock
   'custom-border
   'custom-dialog
   'dialog
   'dnd
   'dynamic-layout
   'editor-pane-hyperlink
   'explorer
   'form
   'forms
   'full-screen
   'game-of-life
   'hotpotatoes
   'j18n
   'kitchensink
   'kotka-bind
   'log-window
   'make-widget
   'mig
   'paintable
   'pan-on-drag
   'pi
   'piano
   'popup
   'reorderable-listbox
   'rpn
   'rsyntax
   'scribble
   'scroll
   'slider
   'spinner
   'swingx
   'table
   'temp
   'text-editor
   'text-ref
   'toggle-listbox
   'tree
   'xyz-panel])

(defn make-frame []
  (frame
    :title "Seesaw Example Launcher"
    :size [200 :by 500]
    :content
      (border-panel
        :hgap 5 :vgap 5 :border 5
        :center (scrollable (listbox-x
                              :id :list
                              :model examples
                              :selection-mode :single
                              :highlighters [(hl-simple-striping)
                                            ((hl-color :background "#88F") :rollover-row)]))
        :south  (button :id :launch :text "Launch"))))

(defn launch-example [s]
  (let [example-ns (str "seesaw.test.examples." s)
        _          (require (symbol example-ns))
        run-fn     (resolve (symbol example-ns "run"))]
    (if run-fn
      (run-fn :dispose)
      (alert (str "Couldn't find function " (symbol example-ns "run"))))))

(defn add-behaviors [f]
  (let [{:keys [list launch]} (group-by-id f)]
    (b/bind
      (b/selection list)
      (b/property launch :enabled?))

    (listen list :mouse-clicked
            (fn [e]
              (when (= 2 (.getClickCount e))
                (launch-example (selection list)))))

    (map-key list "ENTER" (fn [_] (launch-example (selection list))))

    (listen launch :action (fn [_] (launch-example (selection list))))

    (selection! list (first examples)))
  f)

(defexample []
  (dev/debug!)
  (-> (make-frame)
    add-behaviors))

;(run :dispose)

