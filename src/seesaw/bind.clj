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
  (:require [seesaw.core :as ssc])
  (:use [seesaw util]
        [clojure.string :only (capitalize split)]))

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

(defn- get-document-text [^javax.swing.text.Document d]
  (.getText d 0 (.getLength d)))

(extend-protocol Bindable
  clojure.lang.Atom
    (subscribe [this handler]
      (add-watch this (keyword (gensym "bindable-atom-watcher"))
        (fn bindable-atom-watcher
          [k r o n] (when-not (= o n) (handler n)))))
    (notify [this v] (reset! this v))

  javax.swing.text.Document
    (subscribe [this handler]
      (ssc/listen this :document
        (fn [e] (handler (get-document-text this)))))
    (notify [this v] 
      (ssc/invoke-now 
        (when-not (= v (get-document-text this))
          (do
            (.remove this 0 (.getLength this))
            (.insertString this 0 (str v) nil)))))

  javax.swing.BoundedRangeModel
    (subscribe [this handler]
      (ssc/listen this :change
        (fn [e] (handler (.getValue this)))))
    (notify [this v] (ssc/invoke-now (when-not (= v (.getValue this)) (.setValue this v)))))

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

(defn property [^java.awt.Component target property-name]
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

(extend-protocol ToBindable
  javax.swing.JLabel
    (to-bindable* [this] (property this :text))
  javax.swing.JSlider  
    (to-bindable* [this] (.getModel this))
  javax.swing.JProgressBar  
    (to-bindable* [this] (.getModel this))
  javax.swing.text.JTextComponent 
    (to-bindable* [this] (.getDocument this)))


