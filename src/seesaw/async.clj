(ns seesaw.async
  (:use [seesaw core]))


; Wait for an event
;   result is event
; Wait for a specific time
;   no result
; Wait for a future to complete
;   result is value of future
; Wait for an ordered set of events (&&)
;   result is list of results
; Wait for all of a set of events, in any order (&&)
;   result is set of values (or unordered list)
; Wait for any of a set of events (||)
;   result is value of first event
; Wait for an event with timeout -> (wait event || wait time)
;   result is value or nil

;(await-event a :action-performed (fn [e] 
  ;(text! status "Now wait just 3 seconds!")
  ;(config! e :enabled? false)
  ;(wait 3000 (fn []
    ;(text! status "Now click B!")
    ;(await-event b :action-performed (fn [e]
      ;(text! status "Now click C!")
      ;(config! e :enabled? false)
      ;(await-event c :action-performed (fn [e]
        ;(config! e :enabled? false)
        ;(text! status "Done!")))))))))

(defmacro async
  "Macro that executes an async call (the first form), ignores its result, and the 
  executes the body normally."
  [async-call & body]
  `(~@(apply list (first async-call) `(fn [& args#] ~@body) (rest async-call))))

(defmacro async-let
  "Macro that's similar to let, but executes bindings serially as async calls.
  Once the bindings have completed, the body is executed normally"
  [[b async-call & more] & body]
  `(~@(apply list 
        (first async-call) 
        (if more `(fn [~b] (async-let ~more ~@body))
                  `(fn [~b] ~@body))
        (rest async-call))))

(defn await-event 
  "Wait for an event on a widget and call the continuation function with the
  event object when it fires. Must be called inside async.

  Examples:
    (async 
      (await-event my-button :action-performed)
      ... do more stuff ...)
  "
  [continue target event]
  (let [remove-fn (atom nil)
        handler (fn [e] (@remove-fn) (continue e))]
    (reset! remove-fn (listen target event handler))))

(defn wait 
  "Wait the given number of milliseconds and then call the continuation function.
  Must be called inside async.
  
  Examples:
    ; Wait 5 seconds
    (async
      (wait 5000)
      ... do more stuff ...)
  "
  [continue millis]
  (timer 
    (fn [_] (continue))
    :initial-delay millis :repeats? false))

(defn await-future* [continue f]
  (future
    (let [result (f)]
      (invoke-now (continue result)))))

(defmacro await-future 
  "Execute the body in the background and then pass the result to a continuation
  function *in the UI thread*.
  "
  [continue & body]
  `(await-future* ~continue (fn [] ~@body)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def btn-a (button :text "A"))
(def btn-b (button :text "B"))
(def btn-c (button :text "C"))
(def status (label "Start with A"))
(def progress (progress-bar :min 0 :max 100))

(defn aa-wait-bc []
  ; First collect two clicks on button A
  (async-let [e (await-event btn-a :action-performed)
              e (await-event btn-a :action-performed)]
    (config! e :enabled? false)
    (text! status "Now wait just 3 seconds!")
    ; Now wait for a bit asynchronously
    (async 
      (wait 3000)
      (text! status "Now click B!")
      ; Collect a click from button B
      (async-let [e (await-event btn-b :action-performed)]
        (text! status "Now click C!")
        (config! e :enabled? false)
        ; Collect a click from button C
        (async-let [e (await-event btn-c :action-performed)]
          (config! [btn-a btn-b btn-c] :enabled? true)
          (text! status "Done!"))))))

(defn background-task []
  (text! status "Click A to start background task")
  (async 
    ; Wait for button a to be clicked
    (await-event btn-a :action-performed)
    (text! status "Background task running")
    ; Run some code in a background thread and collect the result when
    ; it's done
    (async-let [result (await-future 
                         (loop [n 100]
                           (Thread/sleep 50)
                           (invoke-now (config! progress :value (- 100 n)))
                           (if (> n 0) 
                             (recur (dec n))
                             "YES.")))]
      (text! status (str "Background task complete with result " result)))))

(defn n-clicks-on [btn n]
  (text! btn (format "%d more clicks" n))
  (when (> n 0)
    (async
      (await-event btn :action-performed)
      (n-clicks-on btn (dec n)))))

(invoke-later
  (-> (frame :content (vertical-panel :items [btn-a btn-b btn-c status progress]))
    pack! 
    show!)
  (background-task))

;(aa-wait-bc)
;(n-clicks-on a 5)
;(background-task)

