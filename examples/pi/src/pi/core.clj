;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns pi.core
  (:use seesaw.core)
  (:require seesaw.invoke)
  (:import [java.util.concurrent LinkedBlockingQueue TimeUnit]))

(native!)

(defn calculate-pi-for 
  "Calculate a sliver of pi"
  [start step-size]
  (reduce 
    (fn [acc i]
      (+ acc (/ (* 4.0 (- 1 (* (mod i 2) 2))) (+ (* 2 i) 1))))
    0.0
    (range (* start step-size) (- (* (inc start) step-size) 1))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Agent Actions

(defn agent-calculate [{:keys [queue result] :as state}]
  (when (:running state)
    (if-let [{:keys [start step-size]} (.poll queue 1 TimeUnit/SECONDS)]
      (swap! result
        (fn [{:keys [value count]} new-value]
          {:value (+ value new-value) 
           :count (inc count)})
        (calculate-pi-for start step-size)))
    (send *agent* agent-calculate))
  state)

(defn agent-start [state] 
  (send *agent* agent-calculate) 
  (assoc state :running true))

(defn agent-stop [state] 
  (assoc state :running false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Task setup

(defn init-task [step-size steps result]
  (let [queue  (LinkedBlockingQueue. (for [i (range steps)] {:start i :step-size step-size}))
        agents (for [i (range 4)]
                  (agent {:running false
                          :queue  queue 
                          :result result}))]
    (doseq [a agents] (send a agent-start))
    { :agents agents
      :queue  queue
      :result result}))

(def current-task (atom nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Event handlers

(defn go [e]
  (let [root         (to-root e)
        steps        (-> (select root [:#steps])     text Integer/parseInt)
        step-size    (-> (select root [:#step-size]) text Integer/parseInt)
        result-label (select root [:#result])
        progress     (config! (select root [:#progress]) :max steps :value 0)
        result       (atom {:value 0.0 :count 0})]
    (add-watch result (gensym) 
      (seesaw.invoke/signaller 
        (fn [k r o {:keys [value count]}] 
          (config! progress :value count)
          (text! result-label (format "\u03C0 = %.20f" value)))))
    (reset! current-task (init-task step-size steps result))))

(defn cancel [e]
  (if-let [{:keys [agents queue result]} @current-task]
    (do
      (.clear queue)
      (doseq [a agents]
        (send a agent-stop))
      (reset! current-task nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; User Interface

(def gap [:fill-h 5])

(defn make-frame []
  (frame
    :on-close :exit
    :size [300 :by 300]
    :content (border-panel :class :container
               :north  (toolbar :items ["Steps"     gap (text :id :steps)     gap
                                        "Step size" gap (text :id :step-size) gap
                                        (button :id :go)])
               :center (label :id :result)
               :south  (toolbar :items [(progress-bar :id :progress :min 0 :max 10000) gap
                                        (button :id :cancel)]))))

(def stylesheet {
  [:JFrame] {
    :title "Let's Calculate \u03C0!!" }

  [:.container] {
    :background "#ffefd5"
    :vgap 5
    :hgap 5 }

  [:JToolBar] { 
    :border     5
    :floatable? false
    :opaque?    false }

  [:#steps] { :text 10000 }

  [:#step-size] { :text 10000 }

  [:JButton] { 
    :foreground "#0022DD"
    :background "#ffefd5" }

  [:#go]     { :text "Go!" }
  [:#cancel] { :text "Cancel" }

  [:#progress] { 
    :border 10 }

  [:#result] {
    :text       (format "\u03C0 = %.20f" 0.0)
    :border     5
    :halign     :center
    :font       "MONOSPACE-BOLD-24"
    :foreground "#0022DD" }})

(def behaviors {
  [:#go]     {:action-performed go}
  [:#cancel] {:action-performed cancel}
})

(defn apply-stylesheet [root stylesheet]
  (doseq [[sel style] stylesheet]
    (apply config! (select root sel) (reduce concat style)))
  root)

(defn apply-behaviors [root behaviors]
  (doseq [[sel handlers] behaviors]
    (apply listen (select root sel) (reduce concat handlers)))
  root)

(defn -main [& args]
  (invoke-later
    (-> (make-frame) 
      (apply-stylesheet stylesheet) 
      (apply-behaviors behaviors) 
      pack! 
      show!)))

