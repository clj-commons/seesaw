(ns seesaw.test.examples.dynamic-layout
  (:use seesaw.core
        seesaw.test.examples.example))

; Extremely basic example of a panel that changes what it shows based on
; whether it has enough room.
;
; Random thoughts:
;
; * Probably only works in a container like a grid where everything
;   get the same space. Otherwise modifying what's shown would trigger
;   a recursive layout cascade (probably?)
; * A card panel could be used to rather than :visible? to swap between
;   alternative layouts. 

(defn make-dynamic-panel
  [i]
  (let [
        ; This is the "extra" bit that comes and goes
        extra     (horizontal-panel
                    :items [(label :text (str "Panel " i))
                            (button :text (str "Panel " i))])

        ; This is the full size panel with everything in it
        full-size (border-panel
                   :north (combobox :model ["A" "b" "C" "D"])
                   :center extra)

        ; A wrapper panel. Not sure if this is useful or not.
        panel     (border-panel :center full-size)

        ; Calculate the initial preferred size of the panel so
        ; we know how big it wants to be
        preferred (config panel :preferred-size)]

    ; When the panel is resized (due to layout of container)
    ; turn things on an off based on available space.
    (listen panel
      :component-resized
            (fn [e]
              (let [big-enough? (>= (height e) (.height preferred))]
                (config! extra :visible? big-enough?))))
    panel))

(defn make-vertical-panel []
  (grid-panel :columns 1
              :items (for [i (range 10)] (make-dynamic-panel i))))

(defexample []
  (-> (frame  :content (make-vertical-panel))
    pack!
    show!))

;(run :dispose)

