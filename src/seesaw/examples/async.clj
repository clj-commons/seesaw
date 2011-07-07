(ns seesaw.examples.async
  (:use [seesaw core async]))

(defn start-button [workflow]
  (action :name "Start Workflow"
          :handler (fn [e] (run-async 
                             (async-workflow
                               (doasync (config! e :enabled? false))
                               workflow
                               (doasync (config! e :enabled? true))
                               )))))

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
      :items [(start-button (a-wait-bc-workflow  btn-a btn-b btn-c status))
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
                (Thread/sleep 50)
                (invoke-now (config! progress :value (- 100 n)))
                (if (> n 0) 
                  (recur (dec n))
                  "YES.")))]
    (doasync
      (text! status (str "Background task complete with result " result)))))

(defn background-task-tab []
  (let [start-btn (button :text "Start Task")
        status    (label)
        progress  (progress-bar)]
    (vertical-panel :items [(start-button (background-task start-btn status progress))
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
    (vertical-panel :items [(start-button (n-clicks-on btn 5))
                            :separator
                            btn])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn await-button 
  "Wait for a button to be pressed and pass the given value to the
  continuation"
  [btn value]
  (fn [continue] 
    ((await-event btn :action-performed) (fn [_] (continue value)))))

(defn passcode-workflow
  [digit-buttons cancel-button [output & more] result]
  (if output
    (async-workflow
      ; v will be either the text of the button, or nil for cancel
      [v (apply await-any (conj 
                            (map #(await-button % (text %)) digit-buttons)
                            (await-button cancel-button nil)))]
      (if v
        (async-workflow 
          (doasync (text! output v))
          (passcode-workflow digit-buttons cancel-button more (conj result v)))
        (doasync :canceled)))
    (doasync 
      (alert (str "Got " result))
      result)))

(defn passcode-tab []
  (let [digit-buttons (map #(button :text %1) (range 0 9))
        cancel-button (button :text "Cancel")
        outputs (map (fn [_] (text :font "ARIAL-BOLD 24" :editable? false :halign :center)) (range 0 4))]
    (vertical-panel 
      :items ["4-digit passcode workflow"
              (start-button 
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

(defn -main [& args]
  (invoke-later
    (-> (frame 
          :size [300 :by 300]
          :content (tabbed-panel :tabs [{:title "Passcode" :content (passcode-tab)}
                                        {:title "Linear" :content (a-wait-bc-tab)}
                                        {:title "Background Task" :content (background-task-tab)}
                                        {:title "N Clicks" :content (n-clicks-on-tab)}]))
      show!)))

;(-main)

