(ns seesaw.examples.async
  (:use [seesaw core async]))


(defn controls [workflow]
  (let [active (atom nil)
        stop (action :name "Cancel Workflow"
                     :enabled? false
                     :handler (fn [e]
                                (when-let [a @active] 
                                  (cancel a) 
                                  (reset! active nil))))
        start (action :name "Start Workflow"
          :handler (fn [e] (reset! active (run-async 
                             (async-workflow
                               (doasync (config! e :enabled? false)
                                        (config! stop :enabled? true))
                               [result workflow]
                               (doasync 
                                 (alert e (str "Workflow completed with result: " result))
                                 (config! e :enabled? true)
                                 (config! stop :enabled? false))
                               ) :canceled (fn [_]
                                             (config! e :enabled? true)
                                             (config! stop :enabled? false)
                                             (alert e "Workflow canceled"))))))]
    (horizontal-panel :border 5 :items [start stop]))) 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn a-wait-bc-workflow [btn-a btn-b btn-c status]
  ; First collect two clicks on button A
  (async-workflow
    (doasync
      (text! status "Start by clicking A"))
    [e (await-event btn-a :action-performed)]
    (doasync
      (config! e :enabled? false)
      (text! status "Now wait just 3 seconds!"))
    ; Now wait for a bit asynchronously
    (wait 3000)
    (doasync
      (text! status "Now click B!"))
    ; Collect a click from button B
    [e (await-event btn-b :action-performed)]
    (doasync
      (text! status "Now click C!")
      (config! e :enabled? false))
    ; Collect a click from button C
    [e (await-event btn-c :action-performed)]
    (doasync
      (config! [btn-a btn-b btn-c] :enabled? true)
      (text! status "Done!")
      :done)))

(defn a-wait-bc-tab []
  (let [btn-a (button :text "A")
        btn-b (button :text "B")
        btn-c (button :text "C")
        status (label)]
    (vertical-panel 
      :items [(controls (a-wait-bc-workflow  btn-a btn-b btn-c status))
              :separator
              btn-a btn-b btn-c status])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn background-task [start-btn status progress]
  (async-workflow
    (doasync
      (text! status (str "Click '" (text start-btn) "' to start background task")))
    ; Wait for button a to be clicked
    (await-event start-btn :action-performed)
    (doasync
      (text! status "Background task running"))
    ; Run some code in a background thread and collect the result when
    ; it's done
    [result (await-future 
              (loop [n 100]
                (if-not (.. (Thread/currentThread) isInterrupted)
                  (do
                (Thread/sleep 50)
                (invoke-now (config! progress :value (- 100 n)))
                (if (> n 0) 
                  (recur (dec n))
                  "YES."))
                  "Canceled")))]
    (doasync
      (text! status (str "Background task complete with result " result)))))

(defn background-task-tab []
  (let [start-btn (button :text "Start Task")
        status    (label)
        progress  (progress-bar)]
    (vertical-panel :items [(controls (background-task start-btn status progress))
                            :separator
                            start-btn
                            progress
                            status])))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn n-clicks-on [btn n]
  (async-workflow
    (doasync
      (text! btn (format "%d more clicks" n)))
    [e (await-any (await-event btn :action-performed) (wait 3000))]
    (if (and e (> n 0))
      (n-clicks-on btn (dec n))
      (doasync (text! btn (if e "Done" "Too Slow"))))))

(defn n-clicks-on-tab []
  (let [btn (button)]
    (vertical-panel :items [(controls (n-clicks-on btn 5))
                            :separator
                            btn])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn await-button 
  "Wait for a button to be pressed and pass the given value to the
  continuation"
  [btn value]
  (async-workflow (await-event btn :action-performed) (doasync value)))

(defn passcode-workflow
  [digit-buttons cancel-button [output & more] result]
  (if output
    (async-workflow
      ; v will be either the text of the button, or nil for cancel
      [v (apply await-any (conj 
                            (map #(await-button % (text %)) digit-buttons)
                            (await-button cancel-button nil)))
       :when v]
      (doasync (text! output v))
      (passcode-workflow digit-buttons cancel-button more (conj result v)))
    (doasync 
      result)))

(defn passcode-tab []
  (let [digit-buttons (map #(button :text %1) (range 0 9))
        cancel-button (button :text "Cancel")
        outputs (map (fn [_] (text :font "ARIAL-BOLD 24" :editable? false :halign :center)) (range 0 4))]
    (vertical-panel 
      :items ["4-digit passcode workflow"
              (controls
                (async-workflow
                  (doasync (config! outputs :text ""))
                  (passcode-workflow digit-buttons cancel-button outputs [])))
              :separator
              (border-panel
                :north (grid-panel :border 10 :rows 1 :items outputs)
                :center (grid-panel 
                          :columns 3
                          :items (concat digit-buttons ["" cancel-button ""])))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn countdown-workflow [btn seconds format-str]
  (if (>= seconds 0)
    (async-workflow
      ; Update the button text with time remaining
      (call-async text! btn (format format-str seconds))
      ; Wait for the button or a second
      [e (await-any
          (await-event btn :action-performed)
          (wait 1000))]

      ; Loop. Force end if button was clicked. Hmm.
      (countdown-workflow btn 
                          (if e -1 (dec seconds)) 
                          format-str))
    (doasync :done))) 

(defn countdown-workflow-tab []
  (let [fmt "Restart Computer (%d seconds)"
        btn (button :text fmt)]
    (vertical-panel
      :items ["Countdown button workflow"
              (controls (countdown-workflow btn 10 fmt))
              :separator
              btn])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main [& args]
  (invoke-later
    (-> (frame 
          :size [500 :by 450]
          :content (tabbed-panel :tabs [{:title "Passcode" :content (passcode-tab)}
                                        {:title "Linear" :content (a-wait-bc-tab)}
                                        {:title "Background Task" :content (background-task-tab)}
                                        {:title "N Clicks" :content (n-clicks-on-tab)}
                                        {:title "Countdown" :content (countdown-workflow-tab)}]))
      show!)))

;(-main)

