;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Some functions for binding atoms to various mutable Swing objects
            like models."
      :author "Dave Ray"}
  seesaw.bind
  (:use [seesaw.event]))

(defn bind-atom-to-range-model
  "Connect a BoundedRangeModel to the value of an atom. When the atom is
  changed, the model's value is updated and vice versa. BoundedRangeModel
  is used by sliders, progress bars, spinners, scrollbars, etc. This is used 
  by the :value option on these widget types.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/BoundedRangeModel.html
  "
  [^clojure.lang.Atom target-atom ^javax.swing.BoundedRangeModel model]
  (reset! target-atom (.getValue model))
  (add-watch target-atom (keyword (gensym "bind-atom-to-range-model"))
    (fn [k r old-state new-state]
      (javax.swing.SwingUtilities/invokeAndWait
        (fn []
          (when (not= new-state (.getValue model))
            (.setValue model new-state))))))
  (listen model :change
    (fn [e]
      (let [new-value (.getValue model)]
        (when (not= @target-atom new-value)
          (reset! target-atom new-value)))))
  target-atom)

