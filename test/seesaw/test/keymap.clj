;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.keymap
  (:use [seesaw.keymap]
        [seesaw.core :only [button action]]
        [seesaw.keystroke :only [keystroke]])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]))

(describe map-key
  (testing "a keystroke and action"
    (it "maps the key to the action in :descendants scope by default"
      (let [b (button)
            k (keystroke "A")
            a (action)
            _ (map-key b k a)
            id (.. b (getInputMap javax.swing.JComponent/WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) (get k))]
        (expect (= a (.. b (getActionMap) (get id))))))
    (it "maps the key to the action in the given scope"
      (let [b (button)
            k (keystroke "A")
            a (action)
            _ (map-key b k a :scope :self)
            id (.. b (getInputMap javax.swing.JComponent/WHEN_FOCUSED) (get k))]
        (expect (= a (.. b (getActionMap) (get id)))))))

  (testing "a keystroke and a function"
    (it "maps the key to an action that calls the function"
      (let [b (button)
            k (keystroke "A")
            called (atom nil)
            a (fn [e] (reset! called true))
            _ (map-key b k a)
            id (.. b (getInputMap javax.swing.JComponent/WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) (get k))]
        (.. b (getActionMap) (get id) (actionPerformed nil))
        (expect @called))))

  (it "returns a function that undoes its effect"
        (let [b (button)
              k (keystroke "A")
              called (atom 0)
              a (fn [e] (swap! called inc))
              remove-fn (map-key b k a)
              id (.. b (getInputMap javax.swing.JComponent/WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) (get k))]
          (expect (.. b (getActionMap) (get id)))
          (remove-fn)
          (expect (nil? (.. b (getActionMap) (get id))))))

  (testing "a keystroke and a button"
    (it "maps the key to .doClick on the button"
      (let [k (keystroke "A")
            called (atom nil)
            b (button :listen [:action (fn [_] (reset! called true))])
            _ (map-key b k b)
            id (.. b (getInputMap javax.swing.JComponent/WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) (get k))]
        (.. b (getActionMap) (get id) (actionPerformed nil))
        (expect @called))))
  (it "can assign an :id to a mapping"
    (let [k (keystroke "A")
          b (button)
          _ (map-key b k b :id :foo :scope :global)
          id (.. b (getInputMap javax.swing.JComponent/WHEN_IN_FOCUSED_WINDOW) (get k))]
      (expect (= id :foo)))))

