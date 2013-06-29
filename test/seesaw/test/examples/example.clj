(ns seesaw.test.examples.example
  (:use [seesaw.core :only [config! pack! show! invoke-now]]))

(defmacro defexample
  "Does the boilerplate for an example.

  arg-vec is a binding vector of arguments for the example, usually command-line
  args. body is code which must return an instance of javax.swing.JFrame. If the
  frame's size has not been set at all, pack! is called. Then show! is called.

  Defines two functions:

    run   : takes an on-close keyword and trailing args and runs
            the example.
    -main : calls (run :exit & args). i.e. runs the example and exits when
            closed

  See the plethora of examples in this directory for usage examples.
  "
  [arg-vec & body]
  `(do
     (defn ~'run [on-close# & args#]
       (let [~arg-vec args#
             f# (invoke-now ~@body)]
         (config! f# :on-close on-close#)
         (when (= (java.awt.Dimension.) (.getSize f#))
           (pack! f#))
         (show! f#)))

     (defn ~'-main [& args#]
       (apply ~'run :exit args#))))
