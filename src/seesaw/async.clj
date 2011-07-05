(ns seesaw.async
  (:use
    [seesaw.core :only (listen invoke-now)]
    [seesaw.timer :only (timer)]))

(defn run-async
  "Run an asynchronuous workflow. Returns a promise which may be dereferenced
  for the workflow result if there is any."
  [continue]
  (let [result (promise)]
    (continue #(deliver result %))
    result))

(defn call-async
  "Calls the given function asynchronuously and passes on the result to the
  continuation."
  [f & args]
  #(% (apply f args)))

(defmacro doasync
  "Runs the block asynchronuously and passes on the result to the continuation."
  [& body]
  `(call-async (fn [] ~@body)))

(defmacro let-async
  "Create a binding of locals similar to let. However on the right hand side
  of the bindings are asynchronuous functions. Their results will be bound to
  the locals.

  The locals may be used in later bindings and the body (just as in a let).

  Example:

      (let-async [is-even? (call-async even? 2)
                  result   (doasync
                             (if is-even?
                               (+ 2 1)
                               (- 2 1)))]
        (* result 3))

  Executes the steps and the body asynchronuously and passes on the result
  to the continuation."
  [steps & body]
  (let [global-continue (gensym "global-continue")
        step            (fn [inner [local local-continue]]
                          `(~local-continue (fn [~local] ~inner)))]
    `(fn [~global-continue]
       ~(->> steps
          reverse
          (partition 2)
          (reduce step `(~global-continue (do ~@body)))))))

(defn await-event
  "Awaits the given event on the given target asynchronuously. Passes on the
  event to the continuation."
  [target event]
  (fn [continue]
    (let [remove-fn (promise)
          handler   (fn [evt] (@remove-fn) (continue evt))]
      (deliver (listen target event handler)))))

(defn wait
  "Wait asynchronuously for t milliseconds. Passes on nil to the continuation."
  [t]
  (fn [continue]
    (timer (fn [_] (continue nil))
           :initial-delay millis
           :repeats?      false)))

(defn await-future*
  "Call the function with any additional arguments in a background thread and
  wait asynchronuously for its completion. Passes on the result to the
  continuation.
  See also: await-future-async*"
  [f & args]
  (fn [continue]
    (future
      (let [result (apply f args)]
        (invoke-now
          (continue result))))))

(defmacro await-future
  "Execute the code block in a background thread and wait asynchronuously
  for its completion. Passes on the result to the continuation.
  See also: await-future-async*"
  [& body]
  `(await-future* (fn [] ~@body)))

(defmacro async-workflow
  "Create an asynchronuous workflow. Each step is execute asynchronuously
  and hence must be an asynchronuous call. If a step is a vector, it will
  be interpreted as a one binding let binding the result of the async call
  to the given local in the subsequent steps. Passes on the result of the
  last step to the continuation.

  Example:

      (defn some-workflow
        [button-a button-b status]
        (async-workflow
          (doasync
            (config! button-b :enabled? false))
          [evt (await-event-async button-a :action-performed)]
          (doasync
            (config! evt :enabled? false)
            (config! button-b :enabled? false)
            (text! status \"Now click B.\"))
          (wait-async 3000)
          (await-event-async button-b :action-performed)
          (doasync
             (config! button-a :enabled? true)
             (config! button-b :enabled? false)
             (text! status \"Done!\")
             :done)))
  "
  [& steps]
  (let [local    (gensym "local")
        step     (fn [s] (if (vector? s) s [local s]))
        bindings (vec (mapcat step steps))
        result   (second (rseq bindings))] ; Last local.
    `(let-async ~bindings
       ~result)))
