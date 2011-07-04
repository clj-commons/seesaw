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
