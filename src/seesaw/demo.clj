(ns seesaw.demo
  (:use seesaw.core)
  (:import (javax.swing JFrame JLabel)
           (java.awt Color)))

(def rss-url "http://puredanger.com/tech/wp-content/themes/streamline_30/images/rss.gif")
(def redditor "http://static.reddit.com/reddit.com.header.png")

(doseq [f (JFrame/getFrames)]
  (.dispose f))

(def f (JFrame. "HI"))
(doto f
  (.setContentPane 
    (border-panel :hgap 12 :vgap 15
                  :background Color/ORANGE
                  :border ["Hello there" (empty-border :thickness 15)]
                  :north (horizontal-panel 
                           :items [(make-action #(println "FOO" %) 
                                                :name "Click Me"
                                                :icon rss-url
                                                :tip "Yum!")
                                   "<html>Multi-<br><b>LINE</b></html>"
                                   :fill-h
                                   (make-action #(println "BAR" %) 
                                                :name "And Me"
                                                :icon redditor
                                                :tip "Yum!")])
                  :center (vertical-panel 
                            :items [(label :text "This label acts like a link" 
                                           :on-mouse-clicked (fn [e] (println "CLICK!"))
                                           :on-mouse-entered (fn [e] (.. (.getSource e) (setForeground Color/BLUE) ))
                                           :on-mouse-exited (fn [e] (.. (.getSource e) (setForeground Color/BLACK) ))
                                           )
                                    (text :text "HI")
                                    (scrollable (text :text (apply str (interpose "\n" (range 0 20))) 
                                                      :multi-line? true 
                                                      :editable false))])
                  :east  (JLabel. "East")
                  :west  (vertical-panel :background Color/GREEN
                                         :border (line-border 
                                                  :color Color/YELLOW 
                                                  :thickness 5) 
                                         :items ["A" :fill-v rss-url "C" [:fill-v 45] "D"])
                  :south (horizontal-panel 
                           :border [(line-border :top 5) (line-border :top 10 :color Color/RED)]
                           :items ["A" :fill-h "B" [:fill-h 20] rss-url "C"])))
  (.setSize 400 400)
  (.setVisible true))
