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
  (:use [seesaw core util]
        [clojure.string :only (capitalize split)]))

;http://download.oracle.com/javase/6/docs/api/javax/swing/BoundedRangeModel.html

(defprotocol Bindable
  (subscribe [this handler])
  (notify [this v]))

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
  (loop [source first-source target target more more]
    (subscribe source #(notify target %))
    (when (seq more)
      (recur target (first more) (next more))))
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
      (listen this :document
        (fn [e] (handler (get-document-text this)))))
    (notify [this v] 
      (invoke-now 
        (when-not (= v (get-document-text this))
          (do
            (.remove this 0 (.getLength this))
            (.insertString this 0 (str v) nil)))))

  javax.swing.BoundedRangeModel
    (subscribe [this handler]
      (listen this :change
        (fn [e] (handler (.getValue this)))))
    (notify [this v] (invoke-now (when-not (= v (.getValue this)) (.setValue this v)))))

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
    (notify [this v] (config! target property-name v))))

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

