;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for dealing with layouts. Prefer layout specific constructors in seesaw.core, e.g. border-panel."
      :author "Dave Ray"}
  seesaw.layout

  (:use [seesaw.options :only [option-map
                               default-option bean-option resource-option
                               ignore-option
                               apply-options
                               option-provider]]
        [seesaw.util :only [check-args constant-map]]
        [seesaw.make-widget :only [make-widget*]]
        [seesaw.to-widget :only [to-widget*]])
  (:import [java.awt GridBagConstraints GridBagLayout]))

(defprotocol LayoutManipulation
  (add!* [layout target widget constraint])
  (get-constraint* [layout container widget]))

(defn handle-structure-change
  "Helper. Revalidate and repaint a container after structure change"
  [^java.awt.Component container]
  (doto container
    .revalidate
    .repaint))

(defn add-widget
  ([c w] (add-widget c w nil))
  ([^java.awt.Container c w constraint]
    (let [w* (if w (make-widget* w))]
      (check-args (not (nil? w*)) (str "Can't add nil widget. Original was (" w ")"))
      (.add c ^java.awt.Component w* constraint)
      w*)))

(defn add-widgets
  [^java.awt.Container c ws]
  (.removeAll c)
  (doseq [w ws]
    (add-widget c w))
  (handle-structure-change c))

(def default-items-option
  (default-option
    :items
    #(add-widgets %1 %2)
    #(seq (.getComponents ^java.awt.Container %1))
    "A sequence of widgets to add."))

(def nil-layout-options
  (option-map
    default-items-option))

;*******************************************************************************
; Border Layout

(def ^{:private true}  border-layout-dirs
  (constant-map java.awt.BorderLayout :north :south :east :west :center))
(def ^{:private true}  border-layout-dirs-r (clojure.set/map-invert border-layout-dirs))

(defn- border-panel-items-setter
  [panel items]
  (doseq [[w dir] items]
    (add-widget panel w (border-layout-dirs dir))))

(defn- border-panel-items-getter
  [^java.awt.Container panel]
  (let [layout (.getLayout panel)]
    (map #(vector % (border-layout-dirs-r (get-constraint* layout panel %))) (.getComponents panel))))

(def border-layout-options
  (merge
    (option-map
      (default-option :hgap
        #(.setHgap ^java.awt.BorderLayout (.getLayout ^java.awt.Container %1) %2)
        nil
        ["An integer in pixels"])
      (default-option :vgap
        #(.setVgap ^java.awt.BorderLayout (.getLayout ^java.awt.Container %1) %2)
        nil
        ["An integer in pixels"])
      (default-option :items
        border-panel-items-setter
        border-panel-items-getter
        ['[(label "North") :north (button :text "South") :south]]))
    (apply option-map
           (map
             (fn [[k v]] (default-option k #(add-widget %1 %2 v)))
             border-layout-dirs)) ))

(option-provider java.awt.BorderLayout border-layout-options)


;*******************************************************************************
; Card Layout

(def card-layout-options
  (option-map
    (default-option :items
      (fn [panel items]
        (doseq [[w id] items]
          (add-widget panel w (name id)))))
    (default-option :hgap
      #(.setHgap ^java.awt.CardLayout (.getLayout ^java.awt.Container %1) %2)
      nil
      ["Integer pixels"])
    (default-option :vgap
      #(.setVgap ^java.awt.CardLayout (.getLayout ^java.awt.Container %1) %2)
      nil
      ["Integer pixels"])))

(option-provider java.awt.CardLayout card-layout-options)

;*******************************************************************************
; Flow Layout

(def ^{:private true} flow-align-table
  (constant-map java.awt.FlowLayout :left :right :leading :trailing :center))

(def flow-layout-options
  (option-map
    default-items-option
    (default-option :hgap
      #(.setHgap ^java.awt.FlowLayout (.getLayout ^java.awt.Container %1) %2)
      nil
      ["Integer pixels"])
    (default-option :vgap
      #(.setVgap ^java.awt.FlowLayout (.getLayout ^java.awt.Container %1) %2)
      nil
      ["Integer pixels"])
    (default-option :align
      #(.setAlignment ^java.awt.FlowLayout (.getLayout ^java.awt.Container %1)
                      (get flow-align-table %2 %2))
      nil
      (keys flow-align-table))
    (default-option :align-on-baseline?
      #(.setAlignOnBaseline ^java.awt.FlowLayout (.getLayout ^java.awt.Container %1) (boolean %2))
      'boolean)))

(option-provider java.awt.FlowLayout flow-layout-options)

;*******************************************************************************
; Box Layout

(def ^{:private true} box-layout-dir-table {
  :horizontal javax.swing.BoxLayout/X_AXIS
  :vertical   javax.swing.BoxLayout/Y_AXIS
})

(def box-layout-options
  (option-map
    default-items-option))

(option-provider javax.swing.BoxLayout box-layout-options)

(defn box-layout [dir] #(javax.swing.BoxLayout. % (dir box-layout-dir-table)))

;*******************************************************************************
; Grid Layout

(def grid-layout-options
  (option-map
    default-items-option
    (ignore-option :rows    ["Integer rows"])
    (ignore-option :columns ["Integer columns"])
    (default-option :hgap
      #(.setHgap ^java.awt.GridLayout (.getLayout ^java.awt.Container %1) %2)
      nil
      ["Integer pixels"])
    (default-option :vgap
      #(.setVgap ^java.awt.GridLayout (.getLayout ^java.awt.Container %1) %2)
      nil
      ["Integer pixels"])))

(option-provider java.awt.GridLayout grid-layout-options)

(defn grid-layout [rows columns]
  (java.awt.GridLayout.
    (or rows 0)
    (or columns (if rows 0 1))
    0 0))

;*******************************************************************************
; Grid bag layout

(def ^{:private true} gbc-fill
  (constant-map GridBagConstraints :none :both :horizontal :vertical))

(def ^{:private true} gbc-grid-xy (constant-map GridBagConstraints :relative))

(def ^{:private true} gbc-grid-wh
  (constant-map GridBagConstraints :relative :remainder))

(def ^{:private true} gbc-anchors
  (constant-map GridBagConstraints
    :north :south :east :west
    :northwest :northeast :southwest :southeast :center

    :page-start :page-end :line-start :line-end
    :first-line-start :first-line-end :last-line-start :last-line-end

    :baseline :baseline-leading :baseline-trailing
    :above-baseline :above-baseline-leading :above-baseline-trailing
    :below-baseline :below-baseline-leading :below-baseline-trailing))

(defn- gbc-grid-handler [^GridBagConstraints gbc v]
  (let [x (.gridx gbc)
        y (.gridy gbc)]
    (condp = v
      :next (set! (. gbc gridx) (inc x))
      :wrap    (do
                 (set! (. gbc gridx) 0)
                 (set! (. gbc gridy) (inc y))))
    gbc))

(def ^{:private true} grid-bag-constraints-options
  (option-map
    (default-option :grid gbc-grid-handler)
    (default-option :gridx #(set! (. ^GridBagConstraints %1 gridx)      (get gbc-grid-xy %2 %2)))
    (default-option :gridy #(set! (. ^GridBagConstraints %1 gridy)      (get gbc-grid-xy %2 %2)))
    (default-option :gridwidth #(set! (. ^GridBagConstraints %1 gridwidth)  (get gbc-grid-wh %2 %2)))
    (default-option :gridheight #(set! (. ^GridBagConstraints %1 gridheight) (get gbc-grid-wh %2 %2)))
    (default-option :fill #(set! (. ^GridBagConstraints %1 fill)       (get gbc-fill %2 %2)))
    (default-option :ipadx #(set! (. ^GridBagConstraints %1 ipadx)      %2))
    (default-option :ipady #(set! (. ^GridBagConstraints %1 ipady)      %2))
    (default-option :insets #(set! (. ^GridBagConstraints %1 insets)     %2))
    (default-option :anchor #(set! (. ^GridBagConstraints %1 anchor)     (gbc-anchors %2)))
    (default-option :weightx #(set! (. ^GridBagConstraints %1 weightx)    %2))
    (default-option :weighty #(set! (. ^GridBagConstraints %1 weighty)    %2))))

(option-provider GridBagConstraints grid-bag-constraints-options)

(defn realize-grid-bag-constraints
  "*INTERNAL USE ONLY. DO NOT USE.*

  Turn item specs into [widget constraint] pairs by successively applying
  options to GridBagConstraints"
  [items]
  (second
    (reduce
      (fn [[^GridBagConstraints gbcs result] [widget & opts]]
        (apply-options gbcs opts)
        (vector (.clone gbcs) (conj result [widget gbcs])))
      [(GridBagConstraints.) []]
      items)))

(defn- add-grid-bag-items
  [^java.awt.Container panel items]
  (.removeAll panel)
  (doseq [[widget constraints] (realize-grid-bag-constraints items)]
    (when widget
      (add-widget panel widget constraints)))
  (handle-structure-change panel))

(def form-layout-options
  (option-map
    (default-option :items add-grid-bag-items)))

(option-provider GridBagLayout form-layout-options)


(extend-protocol LayoutManipulation
  java.awt.LayoutManager
    (add!* [layout target widget constraint]
      (add-widget target widget))
    (get-constraint* [layout container widget] nil)

  java.awt.BorderLayout
    (add!* [layout target widget constraint]
      (add-widget target widget (border-layout-dirs constraint)))
    (get-constraint* [layout container widget]
      (.getConstraints layout widget))

  java.awt.CardLayout
    (add!* [layout target widget constraint]
      (add-widget target widget (name constraint)))

    (get-constraint* [layout container widget]))

;;;

(defn add!-impl
  [container subject & more]
  (let [^java.awt.Container container (if container (to-widget* container))
        [widget constraint] (if (vector? subject) subject [subject nil])
        layout (.getLayout container)]
    (add!* layout container widget constraint)
    (when more
      (apply add!-impl container more))
    container))

(defn remove!-impl
  [container subject & more]
  (let [^java.awt.Container container      (if container (to-widget* container))
        ^java.awt.Component subject-widget (if container (to-widget* subject))]
    (.remove container subject-widget)
    (when more
      (apply remove!-impl container more))
    container))

(defn- ^Integer index-of-component
  [^java.awt.Container container widget]
  (loop [comps (.getComponents container) idx 0]
    (cond
      (not comps)              nil
      (= widget (first comps)) idx
      :else (recur (next comps) (inc idx)))))

(defn replace!-impl
  [^java.awt.Container container old-widget new-widget]
  (let [idx        (index-of-component container old-widget)]
    (when idx
      (let [constraint (get-constraint* (.getLayout container) container old-widget)]
        (doto container
          (.remove idx)
          (.add    new-widget constraint idx))))
    container))

