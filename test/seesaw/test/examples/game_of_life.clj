(ns seesaw.test.examples.game-of-life
  (:use [seesaw.core]
        [seesaw.graphics]))

; Adapted from http://tech.puredanger.com/2011/11/17/clojure-and-processing/
; lein run -m seesaw.test.examples.game-of-life

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; GOL

(defn neighbors [[x y]]
  (for [dx [-1 0 1]
        dy (if (zero? dx)
             [-1 1]
             [-1 0 1])]
    [(+ dx x) (+ dy y)]))

(defn live [n alive?]
  (or (= n 3)
      (and (= n 2) alive?)))

(defn step [world]
  (set
   (for [[cell n] (frequencies (mapcat neighbors world))
         :when (live n (world cell))]
     cell)))

(defn life [initial-world]
  (iterate step initial-world))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UI

(def types 
  { :glider  #{[0 1] [1 2] [2 0] [2 1] [2 2]}
    :blinker #{[3 3] [3 4] [3 5]}
    :chicken-wire #{[5 5] [6 5]
                                [7 6] [8 6]} 
    :ten-cell-row (into #{} (for [i (range 10)] [16 (+ i 11)])) })

(def worlds (atom (life (:glider types))))

(defn reset [type]
  (reset! worlds (life (types type))))

(defn draw-grid [c g rows cols cells]
  (let [cellw (/ (width c) cols)
        cellh (/ (height c) cols)]
    (doseq [x (range rows)
            y (range cols)]
      (draw g
            (rect (* x cellw) (* y cellh) cellw cellh)
            (style :background (if (contains? cells [x y]) "#F03232"))))))

(defn make-ui [on-close]
  (frame 
    :title "Game of Life" 
    :on-close on-close
    :size [300 :by 300]
    :content (border-panel
               :north (toolbar :items ["Type: " (combobox :id :type :model (keys types))])
               :center (canvas 
                        :id :canvas 
                        :background :black
                        :paint #(draw-grid %1 %2 32 32 (first @worlds)))
               :south (slider :id :period :min 50 :max 1000 :value 250 :inverted? true))))

(defn add-behaviors [root]
  (let [c      (select root [:#canvas])
        t      (timer (fn [_] 
                        (swap! worlds #(drop 1 %)) 
                        (repaint! c))
                      :delay 250
                      :start? true)] 
    (listen root   :window-closing (fn [_] (.stop t)))
    (listen (select root [:#type]) :selection (fn [e] 
                                                (reset (selection e))
                                                (repaint! c)))
    (listen (select root [:#period]) :change (fn [e] (.setDelay t (selection e)))))
  root)

(defn app [on-close]
  (invoke-later
    (-> (make-ui on-close)
      add-behaviors 
      show!)))

(defn -main [& args]
  (app :exit))

;(app :dispose)

