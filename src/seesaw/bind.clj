;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for binding the value of one thing to another, for example
           synchronizing an atom with changes to a slider."
      :author "Dave Ray"}
  seesaw.bind
  (:refer-clojure :exclude [some])
  (:require [seesaw.core :as ssc]
            [seesaw.invoke :as invoke])
  (:use [clojure.string :only (capitalize split)]))

(defprotocol Bindable
  (subscribe [this handler])
  (notify [this v]))

(defprotocol ToBindable
  (to-bindable* [this]))

(defn to-bindable [target]
  (if (satisfies? Bindable target)
    target
    (to-bindable* target)))

(defn composite 
  "Create a composite bindable from the start and end of a binding chain"
  [start end]
  (reify Bindable
    (subscribe [this handler] (subscribe end handler))
    (notify [this v] (notify start v))))

(defn bind 
  "Creates a chain of listener bindings. When the value of source
  changes it is passed along and updates the value of target.

  Examples:

    ; Bind the text of a text box to an atom. As the user types in
    ; t, the value of a is updated.
    (let [t (text)
          a (atom nil)]
      (bind (.getDocument t) a))

    ; Bind a the value of a slider to an atom, with a transform
    ; that forces the value to [0, 1]
    (let [s (slider :min 0 :max 1)
          a (atom 0.0)]
      (bind s (transform / 100.0) a))

    ; Bind the value of an atom to a label
    (let [a   (atom \"hi\")
          lbl (label)]
      (bind a (transform #(.toUpperCase %)) (property lbl :text))))

  Notes:
    Creating a binding does *not* automatically synchronize the values.

    Circular bindings will usually work.
  "
  [first-source target & more]
  (loop [source (to-bindable first-source) target (to-bindable target) more (seq more)]
    (subscribe source #(notify target %))
    (when more
      (recur target (to-bindable (first more)) (next more))))
      (composite first-source target))

(defn funnel
  "Create a binding chain with several input chains. Provides a
  vector of input values further along the chain.

  Example: Only enable a button if there is some text in both fields.

    (let [t1 (text)
          t2 (text)
          b  (button)]
      (bind
        (funnel
          (property t1 :text)
          (property t2 :text))
        (transform #(every? seq %))
        (property b :enabled?)))
  "
  [& bindables]
  (let [inputs  (map to-bindable bindables)
        invals  (atom (vec (repeat (count inputs) nil)))
        handler (fn [i] (fn [v] (swap! invals assoc i v)))]
    (doseq [[i input] (map-indexed vector inputs)]
      (subscribe input (handler i)))
    (reify
      Bindable
      (subscribe [this handler] (subscribe invals handler))
      (notify [this vs]
        (doseq [[input v] (map vector inputs vs)]
          (notify input v))))))

(defn- get-document-text [^javax.swing.text.Document d]
  (.getText d 0 (.getLength d)))

(extend-protocol Bindable
  clojure.lang.Atom
    (subscribe [this handler]
      (add-watch this (keyword (gensym "bindable-atom-watcher"))
        (fn bindable-atom-watcher
          [k r o n] (when-not (= o n) (handler n)))))
    (notify [this v] (reset! this v))

  clojure.lang.Agent
    (subscribe [this handler]
      (add-watch this (keyword (gensym "bindable-agent-watcher"))
        (fn bindable-agen-watcher
          [k r o n] (when-not (= o n) (handler n)))))
    (notify [this v] (throw (IllegalStateException. "Can't notify an agent!")))

  javax.swing.text.Document
    (subscribe [this handler]
      (ssc/listen this :document
        (fn [e] (handler (get-document-text this)))))
    (notify [this v] 
      (when-not (= v (get-document-text this))
        (do
          (.remove this 0 (.getLength this))
          (.insertString this 0 (str v) nil))))

  javax.swing.BoundedRangeModel
    (subscribe [this handler]
      (ssc/listen this :change
        (fn [e] (handler (.getValue this)))))
    (notify [this v] 
      (when-not (= v (.getValue this)) 
        (.setValue this v))))

(defn b-swap! 
  "Creates a bindable that swaps! an atom's value using the given function each
  time its input changes. That is, each time a new value comes in, 
  (apply swap! atom f new-value args) is called.

  This bindable's value (the current value of the atom) is subscribable.
  
  Example:
  
    ; Accumulate list of selections in a vector
    (bind (selection my-list) (b-swap! my-atom conj))
  "
  [atom f & args]
  (reify Bindable
    (subscribe [this handler] 
      (subscribe atom handler))
    (notify [this v] 
      (apply swap! atom f v args))))

(defn- b-send*
  [send-fn agent f & args]
  (reify Bindable
    (subscribe [this handler] 
      (subscribe agent handler))
    (notify [this v] 
      (apply send-fn agent f v args))))

(defn b-send
  "Creates a bindable that (send)s to an agent using the given function each
  time its input changes. That is, each time a new value comes in, 
  (apply send agent f new-value args) is called.

  This bindable's value (the current value of the atom) is subscribable.
  
  Example:
  
    ; Accumulate list of selections in a vector
    (bind (selection my-list) (b-send my-agent conj))
  "
  [agent f & args]
  (apply b-send* send agent f args))

(defn b-send-off 
  "Creates a bindable that (send-off)s to an agent using the given function each
  time its input changes. That is, each time a new value comes in, 
  (apply send agent f new-value args) is called.

  This bindable's value (the current value of the atom) is subscribable.
  
  Example:
  
    ; Accumulate list of selections in a vector
    (bind (selection my-list) (b-send-off my-agent conj))
  "
  [agent f & args]
  (apply b-send* send-off agent f args))

(def ^{:private true} short-property-keywords-to-long-map
     {:min :minimum
      :max :maximum
      :tip :tool-tip-text})

;; by default, property names' first character will be lowercased when
;; added using a property change listener. For some however, the first
;; character must stay uppercased. This map will specify those exceptions.
(def ^{:private true} property-change-listener-name-overrides {
  "ToolTipText" "ToolTipText"
})

(defn- property-kw->java-name
  "(property-kw->java-name :tip) -> \"ToolTipText\""
  [kw]
  (apply str
          (map capitalize (split (-> (short-property-keywords-to-long-map kw kw)
                                     name
                                     (.replace "?" ""))
                                 #"\-"))))

(defn property 
  "Returns a bindable (suitable to pass to seesaw.bind/bind) that
  connects to a property of a widget, e.g. :foreground, :enabled?,
  etc.

  Examples:

    ; Map the text in a text box to the foreground color of a label
    ; Pass the text through Seesaw's color function first to get
    ; a color value.
    (let [t   (text :text \"white\")
          lbl (label :text \"Color is shown here\" :opaque? true)]
      (bind (.getDocument t)
            (transform #(try (color %) (catch Exception (color 0 0 0))))
            (property lbl :background)))
    
  See:
    (seesaw.bind/bind)
  "
  [^java.awt.Component target property-name]
  (reify Bindable
    (subscribe [this handler] 
      (let [property-name (property-kw->java-name property-name)]
        (.addPropertyChangeListener target
          ; first letter of *some* property-names must be lower-case
          (property-change-listener-name-overrides
              property-name
              (apply str (clojure.string/lower-case (first property-name)) (rest property-name)))
          (reify java.beans.PropertyChangeListener 
            (propertyChange [this e] (handler (.getNewValue e)))))))
    (notify [this v] (ssc/config! target property-name v))))

(defn selection
  "Converts the selection of a widget into a bindable. Applies to listbox,
  table, tree, combobox, checkbox, etc, etc. In short, anything to which
  (seesaw.core/selection) applies.

  options corresponds to the option map passed to (seesaw.core/selection)
  and (seesaw.core/selection)
  
  Examples:
    
    ; Bind checkbox state to enabled state of a widget
    (let [cb (checkbox :text \"Enable\")
          t  (text)]
      (bind (selection cb) (property t :enabled?)))

  See:
    (seesaw.bind/bind)
    (seesaw.core/selection)
    (seesaw.core/selection!)
  "
  ([widget options]
    (reify Bindable
      (subscribe [this handler]
        (ssc/listen widget :selection (fn [_] (-> widget (ssc/selection options) handler))))
      (notify [this v]
        (ssc/selection! widget options v))))
  ([widget]
    (selection widget {})))

(defn transform 
  "Creates a bindable that takes an incoming value v, applies
  (f v args), and passes the result on. f should be side-effect
  free.
 
  See:
    (seesaw.bind/bind)"
  [f & args]
  (let [state (atom {:handlers [] :value nil})]
    (reify Bindable
      (subscribe [this handler]
        (swap! state update-in [:handlers] conj handler))
      (notify [this v]
        (let [new-value (:value (swap! state assoc :value (apply f v args)))]
          (doseq [h (:handlers @state)]
            (h new-value)))))))

(defn b-do*
  "Creates a bindable that takes an incoming value v, executes
  (f v args) and does nothing further. That is, it's the end of the binding
  chain.
  
  See:
    (seesaw.bind/bind)
    (seesaw.bind/b-do)"
  [f & args]
  (reify Bindable
    (subscribe [this handler]
      (throw (UnsupportedOperationException. "Can't subscribe to b-do*")))
    (notify [this v]
      (apply f v args))))

(defmacro b-do
  "Macro helper for (seesaw.bind/b-do*). Takes a single-argument fn-style
  binding vector and a body. When a new value is received it is passed
  to the binding and the body is executes. The result is discarded.
  
  See:
    (seesaw.bind/b-do*)
  "
  [bindings & body]
  `(b-do* (fn ~bindings ~@body)))

(defn some
  "Executes a preducate on incoming value. If the predicate returns a truthy
  value, that value is passed on to the next bindable in the chain. Otherwise,
  nothing is notified.
  
  Examples:
    
    ; Try to convert a text string to a number. Do nothing if the conversion
    ; Fails
    (let [input (text)
          output (slider :min 0 :max 100)]
      (bind input (gate #(try (Integer/parseInt %) (catch Exception nil))) output))

  Notes:
    This works a lot like (clojure.core/some)

  See:
    (clojure.core/some)
  "
  [pred]
  (let [state (atom {:handlers [] :value nil})]
    (reify Bindable
      (subscribe [this handler]
        (swap! state update-in [:handlers] conj handler))
      (notify [this v]
        (let [new-value (:value (swap! state assoc :value (pred v)))]
          (when new-value
            (doseq [h (:handlers @state)]
              (h new-value))))))))

(defn tee
  "Create a tee junction in a bindable chain.
  
  Examples:
  
    ; Take the content of a text box and show it as upper and lower
    ; case in two labels
    (let [t (text)
          upper (label)
          lower (label)]
      (bind (property t :text) 
            (tee (bind (transform #(.toUpperCase %)) (property upper :text))
                 (bind (transform #(.toLowerCase %)) (property lower :text)))))

  See:
    (seesaw.bind/bind)
  "
  [& bindables]
  (reify Bindable
    (subscribe [this handler] (throw (UnsupportedOperationException. "tee bindables don't support subscription")))
    (notify [this v] (doseq [b bindables] (notify b v)))))

(defn notify-when*
  [schedule-fn]
  (let [handlers (atom [])]
    (reify Bindable
      (subscribe [this handler]
        (swap! handlers conj handler))
      (notify [this v]
        (schedule-fn
          (fn [] (doseq [h @handlers] (h v))))))))

(defn notify-later 
  "Creates a bindable that notifies its subscribers (next in chain) on the
  swing thread using (seesaw.invoke/invoke-later). You should use this to
  ensure that things happen on the right thread, e.g. (seesaw.bind/property)
  and (seesaw.bind/selection).
  
  See:
    (seesaw.invoke/invoke-later)
  "
  []
  (notify-when* invoke/invoke-later*))

(defn notify-soon 
  "Creates a bindable that notifies its subscribers (next in chain) on the
  swing thread using (seesaw.invoke/invoke-soon). You should use this to
  ensure that things happen on the right thread, e.g. (seesaw.bind/property)
  and (seesaw.bind/selection).
 
  See:
    (seesaw.invoke/invoke-soon)
  "
  []
  (notify-when* invoke/invoke-soon*))

(defn notify-now 
  "Creates a bindable that notifies its subscribers (next in chain) on the
  swing thread using (seesaw.invoke/invoke-now). You should use this to
  ensure that things happen on the right thread, e.g. (seesaw.bind/property)
  and (seesaw.bind/selection).

  Note that sincel invoke-now is used, you're in danger of deadlocks. Be careful.
  
  See:
    (seesaw.invoke/invoke-soon)
  "
  []
  (notify-when* invoke/invoke-now*))

(extend-protocol ToBindable
  javax.swing.JLabel
    (to-bindable* [this] (property this :text))
  javax.swing.JSlider
    (to-bindable* [this] (.getModel this))
  javax.swing.JProgressBar
    (to-bindable* [this] (.getModel this))
  javax.swing.text.JTextComponent
    (to-bindable* [this] (.getDocument this)))


