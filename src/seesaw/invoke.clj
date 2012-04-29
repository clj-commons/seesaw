;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.invoke
  (:import [javax.swing SwingUtilities]))

(defn invoke-later* [f & args] (SwingUtilities/invokeLater #(apply f args)))

(defn invoke-now* [f & args]
  (let [result (atom nil)]
   (letfn [(invoker [] (reset! result (apply f args)))]
     (if (SwingUtilities/isEventDispatchThread)
       (invoker)
       (SwingUtilities/invokeAndWait invoker))
     @result)))

(defn invoke-soon* 
  [f & args]
  (if (SwingUtilities/isEventDispatchThread)
    (apply f args)
    (apply invoke-later* f args)))

(defmacro invoke-later 
  "Equivalent to SwingUtilities/invokeLater. Executes the given body sometime
  in the future on the Swing UI thread. For example,

    (invoke-later
      (config! my-label :text \"New Text\"))

  Notes:

    (seesaw.core/invoke-later) is an alias of this macro.

  See:
  
    http://download.oracle.com/javase/6/docs/api/javax/swing/SwingUtilities.html#invokeLater(java.lang.Runnable) 
  "
  [& body] `(invoke-later* (fn [] ~@body)))

(defmacro invoke-now
  "Equivalent to SwingUtilities/invokeAndWait. Executes the given body immediately
  on the Swing UI thread, possibly blocking the current thread if it's not the Swing
  UI thread. Returns the result of executing body. For example,

    (invoke-now
      (config! my-label :text \"New Text\"))

  Notes:
    Be very careful with this function in the presence of locks and stuff.

    (seesaw.core/invoke-now) is an alias of this macro.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/SwingUtilities.html#invokeAndWait(java.lang.Runnable) 
  "
  [& body] `(invoke-now*   (fn [] ~@body)))

(defmacro invoke-soon
  "Execute code on the swing event thread (EDT) as soon as possible. That is:

    * If the current thread is the EDT, executes body and returns the result
    * Otherise, passes body to (seesaw.core/invoke-later) and returns nil

  Notes:

    (seesaw.core/invoke-soon) is an alias of this macro.

  See:
    (seesaw.core/invoke-later)
    http://download.oracle.com/javase/6/docs/api/javax/swing/SwingUtilities.html#invokeLater(java.lang.Runnable) 
  "
  [& body] `(invoke-soon* (fn [] ~@body)))

(defn signaller* 
  "Returns a function that conditionally queues the given function (+ args) on 
  the UI thread. The call is only queued if there is not already a pending call
  queued. 
  
  Suppose you're performing some computation in the background and want
  to signal some UI component to update. Normally you'd use (seesaw.core/invoke-later)
  but that can easily flood the UI thread with unnecessary updates. That is,
  only the \"last\" queued update really matters since it will overwrite any
  preceding updates when the event queue is drained. Thus, this function takes
  care of insuring that only one update call is \"in-flight\" at any given
  time.

  The returned function returns true if the action was queued, or false if
  one was already active.

  Examples:

    ; Increment a number in a thread and signal the UI to update a label
    ; with the current value. Without a signaller, the loop would send
    ; updates way way way faster than the UI thread could handle them.
    (defn counting-text-box []
      (let [display (label :text \"0\")
            value   (atom 0)
            signal  (signaller* #(text! display (str @value)))]
        (future
          (loop []
            (swap! value inc)
            (signal)
            (recur)))
        label))

  Note:

    You probably want to use the (seesaw.invoke/signaller) convenience
    form.
  
  See:
    (seesaw.invoke/invoke-later)
    (seesaw.invoke/signaller)
  "
  [f]
  (let [active? (atom false)]
    (fn [& args]
      (let [do-it (compare-and-set! active? false true)]
        (when do-it
          (invoke-later 
            (apply f args) 
            (reset! active? false)))
        do-it))))

(defmacro signaller 
  "Convenience form of (seesaw.invoke/signaller*).
  
  A use of signaller* like this:
  
    (signaller* (fn [x y z] ... body ...))
  
  can be written like this:
  
    (signaller [x y z] ... body ...)
  
  See:
    (seesaw.invoke/signaller*)
  "
  [args & body]
  `(signaller* (fn ~args ~@body)))

