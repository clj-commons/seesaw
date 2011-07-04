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

(defn await-event-async
  "Awaits the given event on the given target asynchronuously. Passes on the
  event to the continuation."
  [target event]
  (fn [continue]
    (let [remove-fn (promise)
          handler   (fn [evt] (@remove-fn) (continue evt))]
      (deliver (listen target event handler)))))

(defn wait-async
  "Wait asynchronuously for t milliseconds. Passes on nil to the continuation."
  [t]
  (fn [continue]
    (timer (fn [_] (continue nil))
           :initial-delay millis
           :repeats?      false)))

(defn await-future-async*
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

(defmacro await-future-async
  "Execute the code block in a background thread and wait asynchronuously
  for its completion. Passes on the result to the continuation.
  See also: await-future-async*"
  [& body]
  `(await-future-async* (fn [] ~@body)))
