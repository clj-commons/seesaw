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
    [seesaw.options :only (bean-option default-option apply-options ignore-options)]
    [seesaw.util :only (resource)]))

(defprotocol ComponentSpec
  (append [this builder] "Add the given component to the form builder"))

(extend-protocol ComponentSpec
  Object
  (append [this builder]
    (.append builder (seesaw.core/make-widget this)))
  String
  (append [this builder]
    (.append builder this))
  clojure.lang.Keyword
  (append [this builder]
    (append (resource this) builder)))

(defn span
  "Add the given component spanning several columns."
  [component column-span]
  (reify
    ComponentSpec
    (append [this builder]
      (.append builder (seesaw.core/make-widget component) column-span))))

(defn next-line
  "Continue with the nth next line in the builder."
  ([] (next-line 1))
  ([n]
   (reify
     ComponentSpec
     (append [this builder]
       (.nextLine builder n)))))

(defn next-column
  "Continue with the nth next column in the builder."
  ([] (next-column 1))
  ([n]
   (reify
     ComponentSpec
     (append [this builder]
       (.nextLine builder n)))))

(defn title
  "Adds the given title to the form."
  [title]
  (reify
    ComponentSpec
    (append [this builder]
      (.appendTitle builder (resource title)))))

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
       (.appendSeparator builder (resource label))))))

(defn group
  "Group the rows of the contained items into a row group."
  [& items]
  (reify
    ComponentSpec
    (append [this builder]
      (.setRowGroupingEnabled builder true)
      (doseq [item items] (append item builder))
      (.setRowGroupingEnabled builder false))))

(def ^{:private true} layout-options
  {:column-groups (default-option :column-groups #(.setColumnGroups %1 (into-array (map int-array %2))))})

(def ^{:private true} ignore-layout-options
  (ignore-options layout-options))

(def ^{:private true} builder-options
  {:items                  (default-option :items #(doseq [item %2] (append item %1)))
   :default-dialog-border? (default-option :default-dialog-border? #(when %2 (.setDefaultDialogBorder %1)))
   :default-row-spec       (bean-option :default-row-spec DefaultFormBuilder)
   :leading-column-offset  (bean-option :leading-column-offset DefaultFormBuilder)
   :line-gap-size          (bean-option :line-gap-size DefaultFormBuilder)
   :paragraph-gap-size     (bean-option :paragraph-gap-size DefaultFormBuilder)})

(def ^{:private true} ignore-builder-options
  (ignore-options builder-options))

(defn ^JPanel forms-panel
  "Construct a panel with a FormLayout. The column spec is
  expected to be a FormLayout column spec in string form.

  The items are a list of strings, components or any of the
  combinators. For example:

      :items [\"Login\" (text) (next-line)
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
    (apply-options layout opts
                   (merge layout-options ignore-builder-options))
    (apply-options builder opts
                   (merge builder-options ignore-layout-options))
    (doto (.getPanel builder)
      (apply-options opts (merge @#'seesaw.core/default-options
                                 ignore-layout-options
                                 ignore-builder-options)))))

