(ns seesaw.async
  (:import
    clojure.lang.IDeref
    clojure.lang.IFn)
  (:use
    [seesaw.core :only (listen invoke-now)]
    [seesaw.timer :only (timer)]))

(defprotocol Async
  (cancel* [this] "Cancel this asynchronuous process."))

(defn cancel
  "Cancel the given asynchronuous event or process."
  [this]
  {:pre [(satisfies? Async this)]}
  (cancel* this))

(defprotocol AsyncEventSource
  (wait-for* [this] "Start waiting for the given event source."))

(defn wait-for
  "Wait asynchronuously for the given event source. Returns the source."
  [this]
  {:pre [(satisfies? AsyncEventSource this)]}
  (wait-for* this))

(defmacro async-fn
  "Create an anonymous function, which is one can cancel, and which thusly
  can act as a continuation for an async process. The function might call
  itself be this, which is captured. The is only one arity possible."
  [args & body]
  `(let [canceled?# (atom false)]
     (reify
       IFn
       (invoke [~'this ~@args]
         (when-not @canceled?#
           ~@body))
       Async
       (cancel* [_this]
         (reset! canceled?# true)))))

(defn run-async
  "Run an asynchronuous workflow. Returns a promise which may be dereferenced
  for the workflow result if there is any."
  [continue]
  (let [result (promise)
        cont   (continue (async-fn [r] (deliver result r)))]
    (wait-for cont)
    (reify
      Async
      (cancel* [this]
        (deliver result ::canceled)
        (cancel cont))
      IDeref
      (deref [this]
        @result))))

(defn call-async
  "Calls the given function asynchronuously and passes on the result to the
  continuation."
  [f & args]
  (fn [continue]
    (reify
      AsyncEventSource
      (wait-for* [this]
        (continue (apply f args)))
      Async
      (cancel* [this]
        (cancel continue)))))

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
  to the continuation.

  Similar to clojure.core/for one may specify a test condition with :when.
  If the condition is true, the let will be continued. Otherwise the
  process will be stopped and nil will be passed on to the continuation.

  Similar to clojure.core/for one may specify normal let bindings with :let.
  This reliefs the need to wrap things in a doasync block. The above example
  could be also written as:

      (let-async [is-even? (call-async even? 2)
                  :let     [result (if is-even?
                                     (+ 2 1)
                                     (- 2 1))]]
        (* result 3))
  "
  [steps & body]
  (let [global-continue (gensym "global-continue")
        current (gensym "current")
        step    (fn [inner [local local-continue]]
                  (condp = local
                    :when `(if ~local-continue
                             ~inner
                             (~global-continue nil))
                    :let  `(let ~local-continue
                             ~inner)
                    `(->> (async-fn [~local] ~inner)
                       (~local-continue)
                       (reset! ~current)
                       wait-for)))]
    `(fn [~global-continue]
       (let [canceled?# (atom false)
             ~current   (atom nil)]
         (reify
           AsyncEventSource
           (wait-for* [_this#]
             (when-not @canceled?#
               ~(->> steps
                  (partition 2)
                  reverse
                  (reduce step `(~global-continue (do ~@body))))))
           Async
           (cancel* [_this#]
             (reset! canceled?# true)
             (when-let [curr# @~current] (cancel curr#))
             (cancel ~global-continue)))))))

(defmacro async-workflow
  "Create an asynchronuous workflow. Each step is execute asynchronuously
  and hence must be an asynchronuous call. If a step is a vector, it will
  be interpreted as a binding step for let-async, eg. binding the result
  of the async call to the given local in the subsequent steps. But :let
  and :when work as well. Passes on the result of the last step to the
  continuation.

  Example:

      (defn some-workflow
        [button-a button-b status]
        (async-workflow
          (call-async config! button-b :enabled? false)
          [evt (await-any
                 (await-event button-a :action-performed)
                 (wait 5000))]
          [:when evt]
          (doasync
            (config! evt :enabled? false)
            (config! button-b :enabled? false)
            (text! status \"Now click B.\"))
          (wait 3000)
          (await-event button-b :action-performed)
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

(defn await-event
  "Awaits the given event on the given target asynchronuously. Passes on the
  event to the continuation."
  [target event]
  (fn [continue]
    (let [remove-fn (promise)]
      (reify
        AsyncEventSource
        (wait-for* [this]
          (let [handler (fn [evt] (@remove-fn) (continue evt))]
            (deliver remove-fn (listen target event handler))))
        Async
        (cancel* [this]
          (@remove-fn)
          (cancel continue))))))

(defn wait
  "Wait asynchronuously for t milliseconds. Passes on nil to the continuation."
  [t]
  (fn [continue]
    (let [tmr (promise)]
      (reify
        AsyncEventSource
        (wait-for* [this]
          (deliver tmr (timer (fn [_] (continue nil))
                              :initial-delay t
                              :repeats?      false)))
        Async
        (cancel* [this]
          (.stop ^javax.swing.Timer @tmr)
          (cancel continue))))))

(defn await-future*
  "Call the function with any additional arguments in a background thread and
  wait asynchronuously for its completion. Passes on the result to the
  continuation.
  See also: await-future"
  [f & args]
  (fn [continue]
    (let [inner-continue (async-fn [result]
                           (invoke-now
                             (continue result)))]
      (reify
        AsyncEventSource
        (wait-for* [this]
          (future
            (inner-continue (apply f args))))
        Async
        (cancel* [this]
          (cancel inner-continue)
          (cancel continue))))))

(defmacro await-future
  "Execute the code block in a background thread and wait asynchronuously
  for its completion. Passes on the result to the continuation.
  See also: await-future*"
  [& body]
  `(await-future* (fn [] ~@body)))

(defn await-any
  "Wait asynchronuously until one of the given events happens. Passes on
  the result of the triggering event to the continuation."
  [& events]
  (fn [global-continue]
    (let [guard  (atom false)
          inner-continue (async-fn [result]
                           (when (compare-and-set! guard false true)
                             (global-continue result)))
          events (for [evt events] (evt inner-continue))]
      (reify
        AsyncEventSource
        (wait-for* [this]
          (doseq [event events] (wait-for event)))
        Async
        (cancel* [this]
          (doseq [event events] (cancel event))
          (cancel global-continue))))))

(defn await-all
  "Wait asynchronuously until all of the given events happened. Passes on
  the sequence of results to the continuation."
  [& events]
  (fn [global-continue]
    (let [n         (count events)
          guard     (atom n)
          promises  (repeatedly n promise)
          inner-continue (fn [p]
                           (async-fn [result]
                             (deliver p result)
                             (when (zero? (swap! guard dec))
                               (global-continue (map deref promises)))))
          events    (for [[event p] (map vector events promises)]
                      (event (inner-continue p)))]
      (reify
        AsyncEventSource
        (wait-for* [this]
          (doseq [event events] (wait-for event)))
        Async
        (cancel* [this]
          (doseq [event events] (cancel event))
          (cancel global-continue))))))

(defn await-valid
  "Wait asynchronuously for the given event. Passes on the result to the
  continuation if pred returns truethy for it. If the result does not
  fulfil the predicate the optional invalid handler is called with the
  continuation and the result. If the invalid handler returns a
  AsyncEventSource, the await-valid will wait again for the new source.
  Otherwise the invalid-handler should return nil and handle the result
  and continuation on its own. The default behaviour for invalid is to
  cycle until a valid result is returned."
  [pred event & {:keys [invalid]}]
  (fn [global-continue]
    (let [invalid   (or invalid (fn [_ _] event))
          current   (atom nil)
          inner-continue (async-fn [result]
                           (if (pred result)
                             (global-continue result)
                             (when-let [event (invalid global-continue result)]
                               (wait-for (reset! current (event this))))))]
      (reset! current (event inner-continue))
      (reify
        AsyncEventSource
        (wait-for* [this] (wait-for @current))
        Async
        (cancel* [this]
          (cancel @current)
          (cancel global-continue))))))

