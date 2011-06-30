(ns seesaw.forms
  (:import
    java.awt.Component
    com.jgoodies.forms.builder.DefaultFormBuilder))

(defprotocol ComponentSpec
  (append [this builder] "Add the given component to the form builder"))

(extend-protocol ComponentSpec
  Component
  (append [this builder]
    (.append builder this))
  String
  (append [this builder]
    (.append builder this)))

(defn span
  "Add the given component spanning several columns."
  [component column-span]
  (reify
    ComponentSpec
    (append [this builder]
      (.append builder component column-span))))

(def next-line
  "Continue with the next line in the builder."
  (reify
    ComponentSpec
    (append [this builder]
      (.nextLine builder))))

(defn title
  "Adds the given titel to the form."
  [title]
  (reify
    ComponentSpec
    (append [this builder]
      (.appendTitle builder this))))

(defn separator
  "Adds a separator with an optional label to the form."
  ([]
   (reify
     ComponentSpec
     (append [this builder]
       (.appendSeparator builder))))
  ([label]
   (reify
     ComponentSpec
     (append [this builder]
       (.appendSeparator builder this)))))
