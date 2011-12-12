(ns seesaw.test.examples.pan-on-drag
  (:use seesaw.test.examples.example)
  (:require [seesaw.core :as sc]
            [seesaw.behave :as behave]))

(defn scrollable-image [id]
  (sc/scrollable
    (sc/label :id id 
              ;:icon "file:///Users/dave/Desktop/IMG_0058.JPG"
              )))

(defn pan [view-to-pan dx dy]
  (let [^javax.swing.JViewport  viewport (.. view-to-pan getParent)
        rect      (.getViewRect viewport)
        full-size (.getViewSize viewport)
        [x y w h] [(.x rect) (.y rect) (.width rect) (.height rect)]
        new-x (Math/min (Math/max 0 (+ x (int dx))) (- (.width full-size) w))
        new-y (Math/min (Math/max 0 (+ y (int dy))) (- (.height full-size) h))]
    (.setViewPosition viewport (java.awt.Point. new-x new-y))))

(defn- calculate-scales [panner view-to-pan]
  [(/ (.getWidth view-to-pan) (.getWidth panner))
   (/ (.getHeight view-to-pan) (.getHeight panner))])

(defn pan-on-drag 
  ([view-to-pan & {:keys [panner speed] :or {panner view-to-pan speed 1.0}}]
    (behave/when-mouse-dragged panner 
      :drag (fn [e [dx dy]] 
              (let [[sx sy] (calculate-scales panner view-to-pan)] 
                (pan view-to-pan (* dx sx speed) (* dy sy speed)))))))

(defn add-behaviors [root]
  (pan-on-drag (sc/select root [:#image]))
  (pan-on-drag (sc/select root [:#image]) 
               :panner (sc/select root [:#panner]) 
               :scale 5.0)
  root)

(defn app []
  (sc/frame
    :title "Pan on drag"
    :size [480 :by 480]
    :content 
     (sc/border-panel 
       :north  "Click and drag to pan the image"
       :center (scrollable-image :image)
       :south  (sc/flow-panel 
                 :items ["Or drag on the blue field to pan the image"
                        (sc/label 
                          :id         :panner
                          :size       [50 :by 50]
                          :background :blue)]))))

(defexample []
  (-> (app) add-behaviors))

;(run :dispose)

