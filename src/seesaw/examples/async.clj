(ns seesaw.examples.async
  (:use [seesaw core async]))

(def btn-a (button :text "A"))
(def btn-b (button :text "B"))
(def btn-c (button :text "C"))
(def status (label "Start with A"))
(def progress (progress-bar :min 0 :max 100))

(defn aa-wait-bc []
  ; First collect two clicks on button A
  (async-workflow
    [e (await-event btn-a :action-performed)]
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

(defn background-task []
  (async-workflow
    (doasync
      (text! status "Click A to start background task"))
    ; Wait for button a to be clicked
    (await-event btn-a :action-performed)
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

(defn n-clicks-on [btn n]
  (async-workflow
    (doasync
      (text! btn (format "%d more clicks" n)))
    [e (await-any (await-event btn :action-performed) (wait 3000))]
    (if (and e (> n 0))
      (n-clicks-on btn (dec n))
      (doasync (text! btn (if e "Done" "Too Slow"))))))

(defn -main [& args]
  (invoke-later
    (-> (frame :content (vertical-panel :items [btn-a btn-b btn-c status progress]))
      pack! 
      show!)
    (run-async (n-clicks-on btn-a 5))
    ;(run-async (background-task))
    ;(run-async (aa-wait-bc))
    ))

;(-main)

