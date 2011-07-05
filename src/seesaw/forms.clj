;  Copyright (c) Dave Ray, Meikel Brandmeyer 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.forms
  (:import
    javax.swing.JPanel
    com.jgoodies.forms.builder.DefaultFormBuilder
    com.jgoodies.forms.layout.FormLayout)
  (:require
    seesaw.core)
  (:use
    [seesaw.util :only (apply-options ignore)]))

(defprotocol ComponentSpec
  (append [this builder] "Add the given component to the form builder"))

(extend-protocol ComponentSpec
  Object
  (append [this builder]
    (.append builder (seesaw.core/to-widget this true)))
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

(def
  ^{:doc "Continue with the next line in the builder."}
  next-line
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
       (.appendSeparator builder label)))))

(defn group
  "Group the rows of the contained items into a row group."
  [& items]
  (reify
    ComponentSpec
    (append [this builder]
      (.setRowGroupingEnabled builder true)
      (doseq [item items] (append item builder))
      (.setRowGroupingEnabled builder false))))

(def ^{:private true} builder-options
  {:items                  #(doseq [item %2] (append item %1))
   :default-dialog-border? #(when %2 (.setDefaultDialogBorder %1))
   :default-row-spec       #(.setDefaultRowSpec %1 %2)
   :leading-column-offset  #(.setLeadingColumnOffset %1 %2)
   :line-gap-size          #(.setLineGapSize %1 %2)
   :paragraph-gap-size     #(.setParagraphGapSize %1 %2)})

(def ^{:private true} ignore-builder-options
  (zipmap (keys builder-options) (repeat ignore)))

(defn ^JPanel forms-panel
  "Construct a panel with a FormLayout. The column spec is
  expected to be a FormLayout column spec in string form.

  The items are a list of strings, components or any of the
  combinators. For example:

      :items [\"Login\" (text) next-line
              \"Password\" (span (text) 3)]

  Takes the following special properties. They correspond
  to the DefaultFormBuilder option of the same name.

      :default-dialog-border?
      :default-row-spec
      :leading-column-offset
      :line-gap-size
      :paragraph-gap-size

  See http://www.jgoodies.com/freeware/forms/index.html"
  {:seesaw {:class `JPanel}}
  [column-spec & opts]
  (let [layout  (FormLayout. column-spec "")
        panel   (#'seesaw.core/construct JPanel opts)
        builder (DefaultFormBuilder. layout panel)]
    (apply-options builder opts builder-options)
    (doto (.getPanel builder)
      (apply-options opts (merge @#'seesaw.core/default-options
                                 ignore-builder-options)))))
