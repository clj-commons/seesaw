;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.cells
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)]
        [seesaw.core]
        [seesaw.cells]
        [seesaw.font]))

(describe default-list-cell-renderer
  (it "proxies a DefaultListCellRenderer which dispatches to a function"
    (let [expected-font (font :name "ARIAL-BOLD-18")
          jlist (javax.swing.JList.)
          render-fn (fn [renderer info] 
                      (config! renderer :foreground java.awt.Color/YELLOW
                                       :text "hi"
                                       :icon nil
                                       :font expected-font))
          r (default-list-cell-renderer render-fn)
          c (.getListCellRendererComponent r jlist nil 0 false false)]
      (expect (= java.awt.Color/YELLOW (.getForeground c)))
      (expect (= "hi" (.getText c)))
      (expect (= nil (.getIcon c)))
      (expect (= expected-font (.getFont c))))))

(describe default-tree-cell-renderer
  (it "proxies a DefaultTreeCellRenderer which dispatches to a function"
    (let [expected-font (font :name "ARIAL-BOLD-18")
          jtree (javax.swing.JTree.)
          render-fn (fn [renderer info] 
                      (config! renderer :foreground java.awt.Color/YELLOW
                                       :text "hi"
                                       :icon nil
                                       :font expected-font))
          r (default-tree-cell-renderer render-fn)
          c (.getTreeCellRendererComponent r jtree 0 false false false 0 false)]
      (expect (= java.awt.Color/YELLOW (.getForeground c)))
      (expect (= "hi" (.getText c)))
      (expect (= nil (.getIcon c)))
      (expect (= expected-font (.getFont c))))))

(describe to-cell-renderer
  (it "throws an exception if it can't make a renderer for a component"
    (try
      (to-cell-renderer (javax.swing.JLabel.) nil) false
      (catch IllegalArgumentException e true)))
  (it "creates a tree cell renderer for a JTree"
    (instance? javax.swing.tree.TreeCellRenderer (to-cell-renderer (javax.swing.JTree.) (fn [r i]))))
  (it "creates a list cell renderer for a JList"
    (instance? javax.swing.ListCellRenderer (to-cell-renderer (javax.swing.JList.) (fn [r i]))))
  (it "creates a list cell renderer for a JComboBox"
    (instance? javax.swing.ListCellRenderer (to-cell-renderer (javax.swing.JComboBox.) (fn [r i])))))


