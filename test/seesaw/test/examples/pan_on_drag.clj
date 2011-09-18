(ns seesaw.test.examples.pan-on-drag
  (:require [seesaw.core :as sc])
  (:require [seesaw.behave :as behave]))

(defn scrollable-image []
  (sc/scrollable
    (sc/label :icon "file:///Users/dave/Desktop/IMG_0058.JPG")))

(defn pan-on-drag [target]
  (behave/when-mouse-dragged target
    :drag (fn [e [dx dy]]
            (let [^javax.swing.JComponent widget   (sc/to-widget e)
                  ^javax.swing.JViewport  viewport (.. widget getParent)
                  rect      (.getViewRect viewport)
                  full-size (.getViewSize viewport)
                  [x y w h] [(.x rect) (.y rect) (.width rect) (.height rect)]
                  new-x (Math/min (Math/max 0 (+ x dx)) (- (.width full-size) w))
                  new-y (Math/min (Math/max 0 (+ y dy)) (- (.height full-size) h))]
              (.setViewPosition viewport (java.awt.Point. new-x new-y))))))

(defn add-behaviors [root]
  (pan-on-drag (first (sc/select root [:JLabel])))
  root)

(defn app []
  (sc/frame
    :title "Pan on drag"
    :size [480 :by 480]
    :content (sc/border-panel :center (scrollable-image))))

(defn -main [& args]
  (->
    (app)
    add-behaviors
    sc/show!))

;(-main)

