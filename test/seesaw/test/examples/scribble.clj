;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.scribble
  (:use [seesaw core color graphics behave]
        seesaw.test.examples.example))

(def colors [:black :white :blue :green :red :yellow :orange :purple nil])

(def state (atom {
  :tool   nil
  :shapes []
  :style  (style :foreground :black :background nil) }))

(defn render [c g]
  (let [{:keys [shapes current-shape]} @state]
    (apply draw g (apply concat shapes))
    (when current-shape
      (apply draw g current-shape))))

(defn start-new-shape [create-shape-fn]
  (fn [state e]
    (let [p (.getPoint e)]
      (assoc state
             :start-point [(.x p) (.y p)]
             :current-shape [(create-shape-fn (.x p) (.y p)) (:style state)]))))

(defn drag-new-shape [update-shape-fn]
  (fn [state e [dx dy]]
    (let [p (.getPoint e)
          [start-x start-y] (:start-point state)]
      (assoc state :current-shape [(update-shape-fn start-x start-y (.x p) (.y p)) (:style state)]))))

(defn finish-new-shape [state event]
  (-> state
    (update-in [:shapes] conj (:current-shape state))
    (assoc :current-shape nil)))

(def tool-handlers {
  :pencil {
    :start (start-new-shape #(path [] (move-to %1 %2)))
    :drag  (fn [state e delta]
             (let [p (.getPoint e)
                   [shape style] (:current-shape state)]
               (.lineTo shape (.x p) (.y p))
               state))
    :finish finish-new-shape
  }
  :line {
    :start  (start-new-shape #(line %1 %2 %1 %2))
    :drag   (drag-new-shape line)
    :finish finish-new-shape
  }
  :rect {
    :start  (start-new-shape #(rect %1 %2 0 0))
    :drag   (drag-new-shape #(rect %1 %2 (- %3 %1) (- %4 %2)))
    :finish finish-new-shape
  }
  :ellipse {
    :start  (start-new-shape #(ellipse %1 %2 0 0))
    :drag   (drag-new-shape #(ellipse %1 %2 (- %3 %1) (- %4 %2)))
    :finish finish-new-shape
  }
})

(defn switch-tool [state source]
  (let [selected? (selection source)]
    (assoc state :tool (if selected? (tool-handlers (id-of source))))))

(defn update-shape-style
  [state source]
  (let [style-key (id-of source) new-value (selection source)]
    (update-in state [:style] update-style style-key new-value)))

(defn dispatch [handler-name]
  (fn [event & args]
    (let [tool (:tool @state)
          handler (tool handler-name)]
      (when handler
        (apply swap! state handler event args)
        (repaint! (select (to-root event) [:#canvas]))))))

(defn add-behaviors [root]
  (let [canvas (select root [:#canvas])
        tools  (button-group :buttons (select root [:.tool]))
        styles (select root [:.style])]
    (listen tools  :selection #(swap! state switch-tool %))
    (listen styles :selection #(swap! state update-shape-style %))
    (when-mouse-dragged canvas
      :start  (dispatch :start)
      :drag   (dispatch :drag)
      :finish (dispatch :finish))
    (doseq [s styles] #(swap! state update-shape-style s))
    (swap! state switch-tool (selection tools)))
  root)

; TODO On OSX (at least) the renderer is not used for the currently
; displayed value, only when the combobox list is displayed
(defn color-cell [this {:keys [value selected?]}]
  (if value
    (config! this :background value
                  :foreground (if (= :white value) :black :white))
    (config! this :text "None")))

(defn make-ui []
  (frame
    :title "Seesaw Scribble"
    :size [800 :by 600]
    :content
      (border-panel
        :border 5
        :north (toolbar
                 :floatable? false
                 :items [(toggle :id :pencil  :class :tool :text "Pencil" :selected? true)
                         (toggle :id :line    :class :tool :text "Line")
                         (toggle :id :rect    :class :tool :text "Rect")
                         (toggle :id :ellipse :class :tool :text "Ellipse")
                         :separator
                         "Width"
                         (combobox :id :stroke :class :style :model [1 2 3 5 8 13 21])
                         "Line"
                         (combobox :id :foreground :class :style :model colors :renderer color-cell)
                         "Fill"
                         (selection! (combobox :id :background :class :style :model colors :renderer color-cell) nil)
                         ])
        :center (scrollable (canvas :id :canvas
                                    :paint render
                                    :preferred-size [500 :by 500]
                                    :background :white)))))

(defexample []
  (-> (make-ui) add-behaviors))

;(run :dispose)

