;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns seesaw.examples.scroll
  (:use seesaw.core)
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

(defn jlist-row [jlist]
  (let [row (text :columns 10)
        go-action  (action :name "Scroll!"
                    :handler (fn [e]
                               (scroll! jlist :to [:row (Integer/valueOf (text row))])
                               (selection! jlist (Integer/valueOf (text row)))))
        go-button (button :action go-action)]
    (bind/bind row
      (bind/transform #(format "(scroll! v :to [:row %s])" %))
      (bind/property go-button :text))
    (text! row "200")
    (horizontal-panel :items ["Row" row go-button])))

(defn jlist []
  (let [jlist (listbox :model (range 0 1000))]
    (test-panel jlist [(top jlist) (bottom jlist) (jlist-row jlist)])))

(defn jtable-op [jtable op-name]
  (let [arg (text :columns 10)
        go-action  (action :name "Scroll!"
                    :handler (fn [e]
                               (scroll! jtable :to [op-name (Integer/valueOf (text arg))])
                               (selection! jtable (Integer/valueOf (text arg)))))
        go-button (button :action go-action)]
    (bind/bind arg
      (bind/transform #(format "(scroll! v :to [%s %s])" op-name %))
      (bind/property go-button :text))
    (text! arg "200")
    (horizontal-panel :items [arg go-button])))

(defn jtable-op-2 [jtable op-name]
  (let [arg0 (text :columns 10)
        arg1 (text :columns 10)
        go-action  (action :name "Scroll!"
                    :handler (fn [e]
                               (scroll! jtable :to [op-name 
                                                    (Integer/valueOf (text arg0))
                                                    (Integer/valueOf (text arg1))])))
        go-button (button :action go-action)]
    (listen #{arg0 arg1} :document 
      (fn [e] 
        (text! go-button (format "(scroll! v :to [%s %s %s])" op-name (text arg0) (text arg1)))))
    (text! [arg0 arg1] "20")
    (horizontal-panel :items [arg0 arg1 go-button])))

(defn jtable []
  (let [columns (map #(-> ( format "c%09d" %) keyword) (range 26))
        jtable (table :model [:columns columns
                              :rows (repeat 500 (into {} (for [c columns] [c 100])))])]
    (test-panel 
      (doto jtable 
        (.setAutoResizeMode javax.swing.JTable/AUTO_RESIZE_OFF)) 
      [(top jtable) (bottom jtable) 
       (jtable-op jtable :row)
       (jtable-op jtable :column)
       (jtable-op-2 jtable :cell)])))

(defn app-panel []
  (tabbed-panel 
    :tabs [{:title "General" :content (general)}
           {:title "listbox" :content (jlist)}
           {:title "table" :content (jtable)}]))

(defn -main [& args]
  (invoke-later
    (-> (frame :title "Seesaw Scroll Demo" :size [600 :by 300] :content (app-panel))
      show!)))

(-main)

