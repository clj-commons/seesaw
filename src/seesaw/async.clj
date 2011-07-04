(ns seesaw.async)

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
