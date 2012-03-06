;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.selector
  (:use seesaw.selector)
  (:require [seesaw.core :as core])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe select
  (it "should find a widget by type, loosely allowing for sub-classing"
    (let [c (core/label)
          d (core/label)
          b (core/toggle)
          p (core/flow-panel :items [c d b])
          f (core/frame :title "select by type" :content p)]
      (expect (= [c d] (select f [:<javax.swing.JLabel>])))
      (expect (= [b] (select f ["<javax.swing.AbstractButton>"])))))

  (it "should find a widget by type, strictly"
    (let [c (proxy [javax.swing.JLabel] []) ; should be ignored
          d (javax.swing.JLabel.) ; not ignored
          b (core/toggle) ; should be ignored
          p (core/flow-panel :items [c d b])
          f (core/frame :title "select by type" :content p)]
      (expect (= [d] (select f [:<javax.swing.JLabel!>])))
      (expect (= nil (seq (select f ["<javax.swing.AbstractButton!>"]))))))

  (it "should find a widget by Java class name"
    (let [c (proxy [javax.swing.JLabel] [])
          d (core/label)
          b (core/toggle)
          p (core/flow-panel :items [c d b])
          f (core/frame :title "select by type" :content p)]
      (expect (= [d] (select f [:JLabel])))
      (expect (= nil (seq (select f ["JRadioButton"]))))))

  (it "should find a widget by class name"
    (let [c (proxy [javax.swing.JLabel] [])
          d (core/label :class :foo)
          b (core/toggle :class #{:foo :bar})
          p (core/flow-panel :items [c d b])
          f (core/frame :title "select by class" :content p)]
      (expect (= [d b] (select f [:.foo])))
      (expect (= [b] (seq (select f [".bar"]))))))

  (it "should find all descendants of a widget"
    (let [c (proxy [javax.swing.JLabel] [])
          d (core/label)
          b (core/toggle)
          p2 (core/flow-panel :items [c])
          p (core/flow-panel :id :p :items [p2 d b])
          f (core/frame :title "select by type" :content p)]
      (expect (= #{c d b p2} (apply hash-set (select f [:#p :*]))))))

  (it "should find direct children of a widget"
    (let [c (proxy [javax.swing.JLabel] [])
          d (core/label)
          b (core/toggle)
          p2 (core/flow-panel :items [c])
          p (core/flow-panel :id :p :items [p2 d b])
          f (core/frame :title "select by type" :content p)]
      (expect (= #{d b p2} (apply hash-set (select f [:#p :> :*]))))))

  (it "should find a frame by #id and return it"
    (let [f (core/frame :id :my-frame)]
      (expect (= [f] (select f [:#my-frame])))))

  (it "should find a widget by #id and returns it"
    (let [c (core/label :id "hi")
          p (core/flow-panel :id :panel :items [c])
          f (core/frame :title "select by id" :content p)]
      (expect (= [c] (select f [:#hi])))
      (expect (= [p] (select f ["#panel"])))))

  (it "should find menu items by id in a frame's menubar"
    (let [m (core/menu-item :id :my-menu :text "my-menu")
          f (core/frame :title "select menu item"
                   :menubar (core/menubar :items [(core/menu :text "File" :items [(core/menu :text "Nested" :items [m])])]))]
      (expect (= [m] (select f [:#my-menu])))))

  (it "should select all of the components in a tree with :*"
    (let [a (core/label) b (core/text) c (core/label)
          p (core/flow-panel :items [a b c])]
      (expect (= [p a b c] (select p [:*]))))))

