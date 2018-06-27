(ns seesaw.wiki.c-4-containers
  (:require [seesaw.core :as sw :refer :all]
            [seesaw.mig :as mig]
            [seesaw.bind :as b]
            [seesaw.wiki.util :as util])
  (:import (javafx.application Platform)
           (java.lang.reflect Constructor Parameter)))


(sw/frame :title "An example"
          :on-close :hide
          :content (sw/flow-panel
                     :align :left
                     :hgap 20
                     :items ["Label" "Another label"])
          :size [640 :by 400]
          :visible? true)


(sw/frame :title "An example"
          :on-close :hide
          :content (sw/grid-panel
                     :border "Properties"
                     :columns 2
                     :items ["Name" (sw/text "Frank")
                             "Address" (sw/text "123 Main St")])
          :size [640 :by 400]
          :visible? true)

(sw/frame :title "An example"
          :on-close :hide
          :content (sw/border-panel :hgap 10 :vgap 10
                                    :center "CENTER"
                                    :north "NORTH"
                                    :south "SOUTH"
                                    :east "EAST"
                                    :west "WEST")
          :size [400 :by 400]
          :visible? true)


(sw/frame :title "An example"
          :on-close :hide
          :content (mig/mig-panel
                     :constraints ["wrap 2"
                                   "[shrink 0]20px[200, grow, fill]"
                                   "[shrink 0]5px[]"]
                     :items [["name:"] [(sw/text "ert")]
                             ["category:"] [(sw/text "ce")]
                             ["date:"] [(sw/text "ass")]
                             ["comment:"] [(sw/text "asda")]])
          :size [400 :by 400]
          :visible? true)


(sw/frame :title "An example"
          :on-close :hide
          :content (let [lw 100, tw 200, wh 25]
                     (form-panel
                       :border 10
                       :items [[(label :text "Name:"
                                       :preferred-size [lw :by wh])
                                :gridy 0 :gridx 0]
                               [(text :text "asdsad"
                                      :preferred-size [tw :by wh])
                                :gridy 0 :gridx 1
                                :gridwidth :remainder]

                               [(label :text "category:"
                                       :preferred-size [lw :by wh])
                                :gridy 1 :gridx 0
                                :gridwidth 1]
                               [(text :text "asdsad"
                                      :preferred-size [tw :by wh])
                                :gridy 1 :gridx 1
                                :gridwidth :remainder]

                               [(label :text "date:"
                                       :preferred-size [lw :by wh])
                                :gridy 2 :gridx 0
                                :gridwidth 1]
                               [(text :text "asdsad"
                                      :preferred-size [tw :by wh])
                                :gridy 2 :gridx 1
                                :gridwidth :remainder]

                               [(label :text "comment:"
                                       :preferred-size [lw :by wh])
                                :gridy 3 :gridx 0
                                :gridwidth 1]
                               [(text :text "asdsad"
                                      :preferred-size [tw :by wh])
                                :gridy 3 :gridx 1
                                :gridwidth :remainder]
                               ]))
          :size [400 :by 400]
          :visible? true)


(let [open-action (action
                    :handler (fn [e] (alert "I should open a new something."))
                    :name "Open ..."
                    :key "menu O"
                    :tip "Open a new something something.")
      exit-action (action
                    :handler (fn [e] (.dispose (to-frame e)))
                    :name "Exit"
                    :tip "Close this window")]
  (frame
    :title "Toolbar action test"
    :content (border-panel
               :north (toolbar :items [open-action exit-action])
               :center "Insert content here")
    :visible? true))



(input "Pick a city"
       :choices [{:name "New York" :population 8000000}
                 {:name "Ann Arbor" :population 100000}
                 {:name "Twin Peaks" :population 5201}]
       :to-string :name)

(alert "Something terrible has happened")


(def f (let [bg (button-group)
             f  (-> (frame :title "test"
                           :content (border-panel :center (text :id :text :text "hi")
                                                  :south (horizontal-panel :items
                                                                           [(radio :group bg :text "enabled")
                                                                            (radio :group bg :text "disabled")])))
                    pack!
                    show!)]

         (b/bind (b/selection bg)
                 ; when a radio button is clicked the selection temporarily goes to nil
                 ; so filter those out
                 (b/filter identity)
                 (b/transform #(= "enabled" (text %)))
                 (b/property (select f [:#text]) :enabled?))
         f))

(let [open-action (action
                    :handler (fn [e] (alert "I should open a new something."))
                    :name "Open ..."
                    :key "menu O"
                    :tip "Open a new something something.")
      exit-action (action
                    :handler (fn [e] (.dispose (to-frame e)))
                    :name "Exit"
                    :tip "Close this window")]
  (frame :title "MENUS!"
         :menubar
         (menubar :items
                  [(menu :text "File" :items [open-action exit-action])
                   (menu :text "Edit" :items [open-action exit-action])])
         :visible? true))


(let [open-action (action
                    :handler (fn [e] (alert "I should open a new something."))
                    :name "Open ..."
                    :key "menu O"
                    :tip "Open a new something something.")
      exit-action (action
                    :handler (fn [e] (.dispose (to-frame e)))
                    :name "Exit"
                    :tip "Close this window")]
  (frame :title "MENUS!"
         :content (listbox :popup (fn [e] [open-action exit-action])
                           :model ["a" "b"])
         :visible? true
         :size [640 :by 400]))

(frame :title "MENUS!"
       :content (table
                  :model [
                          :columns [{:key :name, :text "Name"} :likes]
                          :rows '[["Bobby" "Laura Palmer"]
                                  ["Agent Cooper" "Cherry Pie"]
                                  {:likes "Laura Palmer" :name "James"}
                                  {:name "Big Ed" :likes "Norma Jennings"}]])
       :visible? true
       :size [640 :by 400])


(javax.swing.JButton.)
(sw/construct javafx.stage.Stage)
(javafx.stage.Stage.)


(javafx.embed.swing.JFXPanel.)
(javafx.application.Platform/setImplicitExit false)

(defn -run-later [^java.lang.Runnable fn]
  (if (Platform/isFxApplicationThread)
    (do
      (println  "aaa")
      (fn))
    (Platform/runLater fn)))

(defmacro run-later [& body]
  `(-run-later
     (fn []
       (try ~@body
            (catch Throwable ex#
              (println ex#))))))

(run-later
  (type javafx.scene.control.Button))


(com.sun.javafx.application.PlatformImpl/startup (fn [_] (javafx.stage.Stage .)))

(defn ctor-fn [^Class k]
  (let [^Constructor ctor (->> (.getDeclaredConstructors k)
                               (sort-by #(count (.getParameters ^Constructor %)))
                               first)
        _                 (assert ctor (str "No ctor for class " k))]
    (.setAccessible ctor true)
    (fn []
      (let [^objects arr (into-array Object (map
                                              (fn [^Parameter p]
                                                (convert-value default-value (.getType p)))
                                              (.getParameters ctor)))]
        (.newInstance ctor arr)))))