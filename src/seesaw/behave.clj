;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "A collection of basic behaviors that can be dynamically added to
           widgets. Most cover basic functionality that's missing from Swing
           or just a pain to implement."
      :author "Dave Ray"}
  seesaw.behave
  (:use [seesaw core]
        [seesaw.util :only [to-seq]]))

(defn when-focused-select-all
  "A helper function which adds a \"select all when focus gained\" behavior to one
  or more text widgets or editable comboboxes.

  Like (seesaw.core/listen) returns a function which will remove all event handlers
  when called.

  Examples:

    (flow-panel :items [
      \"Enter some text here: \"
      (doto
         (text \"All this text will be selected when I get keyboard focus\")
         when-focused-select-all)])

  See:
  "
  [w]
  (let [to-text #(if (instance? javax.swing.JComboBox %)
                          (.. ^javax.swing.JComboBox % getEditor getEditorComponent) %)
        targets (map #(-> % to-widget to-text) (to-seq w))]
    (listen targets :focus-gained
      ; TODO is it safe to assume JTextComponent here? Other option is
      ; to extend selection stuff to text widgets
      #(.selectAll ^javax.swing.text.JTextComponent (to-widget %)))))

(defn when-mouse-dragged
  "A helper for handling mouse dragging on a widget. This isn't that complicated,
  but the default mouse dragged event provided with Swing doesn't give the delta
  since the last drag event so you end up having to keep track of it. This function
  takes three options:

    :start event handler called when the drag is started (mouse pressed).
    :drag  A function that takes a mouse event and a [dx dy] vector which is
           the change in x and y since the last drag event.
    :finish event handler called when the drag is finished (mouse released).

  Like (seesaw.core/listen) returns a function which will remove all event handlers
  when called.

  Examples:
    See (seesaw.examples.xyz-panel)
  "
  [w & opts]
  (let [{:keys [start drag finish]
         :or   { start (fn [e]) drag (fn [e [x y]]) finish (fn [e]) }} opts
        last-point (java.awt.Point.)]
    (listen w
      :mouse-pressed
        (fn [^java.awt.event.MouseEvent e]
          (.setLocation last-point (.getPoint e))
          (start e))
      :mouse-dragged
        (fn [^java.awt.event.MouseEvent e]
          (let [p (.getPoint e)]
            ; TODO the delta reported here is incorrect if the widget is
            ; programmatically moved during the callback. See xyz-panel test.
            (drag e [(- (.x p) (.x last-point)) (- (.y p) (.y last-point))])
            (.setLocation last-point (.getPoint e))))
      :mouse-released
        finish)))

