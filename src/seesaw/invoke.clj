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

(defn invoke-later* [f & args] (SwingUtilities/invokeLater (apply f args)))

(defn invoke-now* [f & args]
  (let [result (atom nil)]
   (letfn [(invoker [] (reset! result (apply f args)))]
     (if (SwingUtilities/isEventDispatchThread)
       (invoker)
       (SwingUtilities/invokeAndWait invoker))
     @result)))

(defmacro invoke-later 
  "Equivalent to SwingUtilities/invokeLater. Executes the given body sometime
  in the future on the Swing UI thread. For example,

    (invoke-later
      (config! my-label :text \"New Text\"))

  Notes:

    (seesaw.core/invoke-now) is an alias of this macro.

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

