(ns pi.core
  (:use seesaw.core)
  (:require seesaw.invoke))

(native!)


(defn calculate-pi-for [start num-elements]
  (reduce 
    (fn [acc i]
      (+ acc (/ (* 4.0 (- 1 (* (mod i 2) 2))) (+ (* 2 i) 1))))
    0.0
    (range (* start num-elements) (- (* (inc start) num-elements) 1))))

(defn calculate [report-fn num-elements num-messages]
  (map 
    #(future (report-fn (calculate-pi-for % num-elements)))
    (range num-messages)))


(def current-task (atom nil))

    
(defn go [e]
  (let [root         (to-root e)
        steps        (-> (select root [:#steps])     text Integer/parseInt)
        step-size    (-> (select root [:#step-size]) text Integer/parseInt)
        result-label (select root [:#result])
        progress     (config! (select root [:#progress]) :max steps :value 0)
        result-atom  (atom {:value 0.0 :count 0})
        update-fn    (seesaw.invoke/signaller (fn [value count] 
                                  (config! progress :value count)
                                  (text! result-label (format "\u03C0 = %.20f" value))))
        report-fn #(swap! result-atom 
                          (fn [{:keys [value count]} new-value] 
                            {:value (+ value new-value) :count (inc count)}) 
                          %)]
    (add-watch result-atom (gensym) (fn [k r o {:keys [value count]}] (update-fn value count)))
    (future (doall (reset! current-task (calculate report-fn step-size steps))))))

(defn cancel [e]
  (future 
    (doseq [f @current-task]
      (future-cancel f))
    (reset! current-task nil)))

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

