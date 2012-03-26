;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.piano
  (:use [seesaw core border]
        seesaw.test.examples.example))

(def num-octaves 7)
(def piano-key-info
  (map
    (fn [index octave note offset color]
      {:index index
       :note  note
       :midi (+ 21 index)
       :color color
       :x-pos (+ offset (* 7 octave))})
    (range)
    (mapcat #(repeat 12 %) (range num-octaves))
    (cycle [:A :A# :B :C :C# :D :D# :E :F :F# :G :G#])
    (cycle [0  0.75 1  2  2.75 3  3.75 4  5  5.75 6  6.75])
    (cycle [:w :b  :w :w :b  :w :b  :w :w :b  :w :b])))

(defn make-piano-key
  [{:keys [index note midi x-pos color] :as info}]
  (let [black? (= :b color)
        background (if black? :black :white)
        pressed    (if black? :darkgrey :lightgrey)]
    (label :user-data  info
           :class      :piano-key
           :background background
           :border     (if black? nil (line-border))
           ; for whatever reason, this causes the white keys to jump in front of
           ; the black keys when pressed. grrr.
           ;:listen [:mouse-pressed #(config! % :background pressed)
                    ;:mouse-released #(config! % :background background)]
           )))

(defn layout-piano-keys [piano]
  (let [piano-keys (config piano :items)
        w          (width piano)
        h          (height piano)
        dx         (quot w (* 7 num-octaves))]
    (doseq [k piano-keys]
      (let [{:keys [x-pos color]} (user-data k)
            black? (= color :b)]
        (config! k :bounds [(* x-pos dx) 0
                            (if black? (quot dx 2) dx)
                            (if black? (quot h 2) h)])))
    (doseq [k (filter #(= :b (:color (user-data %))) piano-keys)]
      (move! k :to-front))))

(defn make-piano []
  (xyz-panel
    :id :piano
    :items (map make-piano-key piano-key-info)
    :listen [:component-resized layout-piano-keys]))

(defn add-behaviors [root]
  (doseq [k (select root [:.piano-key])]
    (listen k
      :mouse-pressed (fn [e]
                       (let [{:keys [note midi]} (user-data e)]
                         (println "You hit " note "/" midi)))))
  root)

(defexample []
  (-> (frame
        :title   "Seesaw piano example"
        :content (border-panel
                   :vgap 5
                   :north "Make a piano out of widgets - clicks go to console"
                   :center (make-piano))
        :size    [900 :by 100])
    add-behaviors))

;(run :dispose)

