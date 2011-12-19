;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for mapping key strokes to actions."
      :author "Dave Ray"}
  seesaw.keymap
  (:use [seesaw.util :only [illegal-argument]]
        [seesaw.keystroke :only [keystroke]]
        [seesaw.action :only [action]]))

(defn- ^javax.swing.Action to-action [act]
  (cond
    (instance? javax.swing.Action act) act

    (instance? javax.swing.AbstractButton act)
      (let [^javax.swing.AbstractButton b act]
        (action :handler (fn [_] (.doClick b))))

    (fn? act) 
      (action :handler act)
    :else (illegal-argument "Don't know how to make key-map action from '%s'" act)))

(defn- ^javax.swing.JComponent to-target [target]
  (cond
    (instance? javax.swing.JComponent target) target
    (instance? javax.swing.JFrame target) (.getRootPane target)
    :else (illegal-argument "Don't know how to map keys on '%s'" target)))

(def ^{:private true} scope-table
  { :descendants javax.swing.JComponent/WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
    :local       javax.swing.JComponent/WHEN_FOCUSED
    :global      javax.swing.JComponent/WHEN_IN_FOCUSED_WINDOW })

(def ^{:private true} default-scope (:descendants scope-table))

(defn map-key
  [target key act & {:keys [scope] :as opts}]
  (let [target (to-target target)
        scope  (scope-table scope default-scope)
        im     (.getInputMap target scope)
        am     (.getActionMap target)
        act    (to-action act)
        id     act]
    (.put im (keystroke key) id)
    (.put am id act)
    ; TODO return value
    ))

