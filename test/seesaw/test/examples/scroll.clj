;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns seesaw.test.examples.scroll
  (:use seesaw.core
        seesaw.test.examples.example)
  (:require [seesaw.bind :as bind]
            seesaw.scroll))

(defn top [target]
  (action :name "(scroll! v :to :top)" 
          :handler (fn [e] (scroll! target :to :top))))

(defn bottom [target]
  (action :name "(scroll! v :to :bottom)" 
          :handler (fn [e] (scroll! target :to :bottom))))

(defn point [target & [x y]]
  (action :name (format "(scroll! v :to [:point %d %d]" x y)
                      :handler (fn [e] (scroll! target :to [:point x y]))))

(defn rect [target & [x y w h]]
  (action :name (format "(scroll! v :to [:rect %d %d %d %d]" x y w h)
                      :handler (fn [e] (scroll! target :to [:rect x y w h]))))

(defn test-panel [target items]
  (border-panel :center (scrollable target) 
                :south (grid-panel :columns 2 
                                    :items items)))
(defn general []
  (let [t (text :multi-line? true :text "Paste a lot of text here so there's scroll bars")]
    (test-panel t [(top t) (bottom t) (point t 500 500) (rect t 0 1500 50 50)])))

(defn test-op-int [target op-name]
  (let [arg (text :columns 10)
        go-action  (action :name "Scroll!"
                    :handler (fn [e]
                               (scroll! target :to [op-name (Integer/valueOf (text arg))])
                               #_(selection! target (Integer/valueOf (text arg)))))
        go-button (button :action go-action)]
    (bind/bind arg
      (bind/transform #(format "(scroll! v :to [%s %s])" op-name %))
      (bind/property go-button :text))
    (text! arg "200")
    (horizontal-panel :items [(name op-name) arg go-button])))

(defn test-op-int-int [target op-name]
  (let [arg0 (text :columns 10)
        arg1 (text :columns 10)
        go-action  (action :name "Scroll!"
                    :handler (fn [e]
                               (scroll! target :to [op-name 
                                                    (Integer/valueOf (text arg0))
                                                    (Integer/valueOf (text arg1))])))
        go-button (button :action go-action)]
    (listen #{arg0 arg1} :document 
      (fn [e] 
        (text! go-button (format "(scroll! v :to [%s %s %s])" op-name (text arg0) (text arg1)))))
    (text! [arg0 arg1] "20")
    (horizontal-panel :items [arg0 arg1 go-button])))

(defn jlist []
  (let [jlist (listbox :model (range 0 1000))]
    (test-panel jlist [(top jlist) (bottom jlist) (test-op-int jlist :row)])))

(defn jtable []
  (let [columns (map #(-> ( format "c%09d" %) keyword) (range 26))
        jtable (table :auto-resize :off 
                      :model [:columns columns
                              :rows (repeat 500 (into {} (for [c columns] [c 100])))])]
    (test-panel 
      jtable 
      [(top jtable) (bottom jtable) 
       (test-op-int jtable :row)
       (test-op-int jtable :column)
       (test-op-int-int jtable :cell)])))

(defn jtext[]
  (let [t (text :multi-line? true 
                :text (apply str (interpose "\n" (range 0 1000))))]
    (test-panel t [(top t) 
                   (bottom t) 
                   (test-op-int t :line) 
                   (test-op-int t :position)])))

(defn app-panel []
  (tabbed-panel 
    :tabs [{:title "general" :content (general)}
           {:title "listbox" :content (jlist)}
           {:title "table"   :content (jtable)}
           {:title "text"    :content (jtext)}]))

(defexample []

  (frame :title "Seesaw Scroll Demo" :size [800 :by 400] :content (app-panel)))

;(run :dispose)

