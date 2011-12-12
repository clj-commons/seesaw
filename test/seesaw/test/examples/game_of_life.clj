(ns seesaw.test.examples.game-of-life
  (:use [seesaw.dev]
        [seesaw.core]
        [seesaw.graphics]
        [seesaw.behave]
        [seesaw.border]
        seesaw.test.examples.example)
  (:require [clojure.java.io :as jio]
            [seesaw.dnd :as dnd]
            [seesaw.bind :as b]))

(debug!)
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
;; Cell file reading 

(defn load-cell-file 
  "Parse a cell file from  http://www.bitstorm.org/gameoflife/lexicon/cells/

    !Name: T-nosed_p4
    !
    .....O.....
    .....O.....
    ....OOO....
    ...........
    ...........
    ...........
    ...OOOOO...
    ..O.OOO.O..
    ..O.O.O.O..
    .OO.O.O.OO.
    O..OO.OO..O
    OO.......OO
  "
  [readerable]
  (with-open [r (jio/reader readerable)]
    (let [lines (line-seq r) 
          name (first lines)
          grid (drop 2 lines)]
      (->> grid
        (map-indexed (fn [y s] 
                       (map-indexed (fn [x c] 
                                      (if (= \O c) [x y])) 
                                    s)))
        (apply concat)
        (filter identity)
        (into #{})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UI

(def glider #{[0 1] [1 2] [2 0] [2 1] [2 2]})
(def worlds (atom (life glider)))

(defn reset [new-cells]
  (reset! worlds (life new-cells)))

(defn draw-grid [c g [minx miny maxx maxy :as bounds] cells]
  (let [cols (- maxx minx)
        rows (- maxy miny)
        cellw (/ (width c) cols)
        cellh (/ (height c) rows)]

    (doseq [x (range minx maxx)
            y (range miny maxy)]
      (draw g
            (rect (* (- x minx) cellw) (* (- y miny) cellh) cellw cellh)
            (style :background (cond
                                 (contains? cells [x y]) "#F03232"
                                 (not= (even? x) (even? y)) "#323232"))))))

(defn make-ui []
  (frame 
    :title "Game of Life" 
    :size [300 :by 300]
    :content (border-panel
               :border 5 :hgap 5 :vgap 5
               :north  "Drag and Drop .cell file URLs on this window"
               :center (canvas :id :canvas 
                               :background :black
                               :border (line-border :thickness 2 :color :black))
               :east   (slider :id :size 
                               :min 8 
                               :max 256 
                               :value 32 
                               :orientation :vertical
                               :tip "Adjust grid size")
               :south  (toolbar :floatable? false 
                                :items [(label :id :link
                                               :tip "Open cell file library in browser"
                                               :text "Cell file library"
                                               :foreground :blue
                                               :cursor :hand)
                                        :separator
                                        "Period (ms) " (spinner 
                                                          :id :period 
                                                          :model (spinner-model 250 :from 50 :to 1000 :by 25))]))))

(defn add-behaviors [root]
  (let [c      (select root [:#canvas])
        t      (timer (fn [_] 
                        (swap! worlds #(drop 1 %)) 
                        (repaint! c))
                      :delay 250
                      :start? true)
        bounds (atom [0 0 32 32])
        drop-fn (fn [{:keys [data]}] (reset (load-cell-file (first data))))] 

    ; Handle dropped uris and files
    (config! root :transfer-handler [:import [dnd/uri-list-flavor  drop-fn
                                              dnd/file-list-flavor drop-fn]])
    
    ; Draw the grid using current bounds
    (config! c :paint #(draw-grid %1 %2 @bounds (first @worlds)))

    ; Clean up timer on close (useful in repl)
    (listen root   :window-closing (fn [_] (.stop t)))

    ; Resize the grid when the size slider changes
    (b/bind
      (b/selection (select root [:#size]))
      (b/b-swap! bounds #(vector (first %1) (second %1) %2 %2)))

    ; Make a fake link
    (listen (select root [:#link])
      :mouse-clicked (fn [_] 
                       (.. (java.awt.Desktop/getDesktop) (browse (java.net.URI. "http://www.bitstorm.org/gameoflife/lexicon/cells/")))))

    ; When the period changes, adjust the timer
    (listen (select root [:#period]) :selection 
      (fn [e] (.setDelay t (selection e)))) 

    ; When mouse is dragged, pan the grid (kinda crappy due to scaling)
    (when-mouse-dragged c
      :drag (fn [e [dx dy]]
              (swap! bounds (fn [[x0 y0 x1 y1]] 
                              [(- x0 dx) (- y0 dy) (- x1 dx) (- y1 dy)])))))

  root)

(defexample []
  (-> (make-ui) add-behaviors))

;(run :dispose)

