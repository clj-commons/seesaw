;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.log-window
  (:use [seesaw.core]
        [seesaw.widgets.log-window]
        [seesaw.invoke :only [signaller]]
        [seesaw.options :only [apply-options]]
        seesaw.test.examples.example)
  (:require [seesaw.dev :as dev]))

; Example usage of seesaw.widgets.log-window/log-window

(dev/debug!)

(defn make-frame []
  (frame
    :title "Log Window Example"
    :size [640 :by 480]
    :content (border-panel
               :center (scrollable (log-window :id :log-window
                                               :limit nil))
               :south (horizontal-panel 
                        :items [(button :id :start :text "Start Spammer")
                                (button :id :stop :text "Stop")
                                (checkbox :id :limit? :text "Limit to")
                                (spinner :id :limit 
                                         :model (spinner-model 500 :from 1 :to nil :by 1))
                                "chars"]))))

(defn spammer [lw prefix go]
  (loop [i 0] 
    (log lw (str prefix " - " i " asdf asdf asdf asdf asdf asdf\n"))
    (Thread/sleep 100)
    (if @go
      (recur (inc i)))))

(defn add-behaviors [f]
  (let [{:keys [log-window start stop limit limit?]} (group-by-id f)
        go (atom false)]
    (listen
      limit?
      :selection (fn [_] (config! log-window :limit (if (value limit?) 
                                              (value limit)))))
    (listen
      stop
      :action (fn [_] (reset! go false)))
    (listen 
      start 
      :action (fn [_] 
                (reset! go true)
                (future (spammer log-window (System/currentTimeMillis) go)))))
  f)

(defexample []
  (-> (make-frame)
    add-behaviors))
;(run :dispose)
