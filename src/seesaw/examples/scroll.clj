;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns seesaw.examples.scroll
  (:use seesaw.core))

(defn general []
  (let [t (text :multi-line? true :text "Paste a lot of text here so there's scroll bars")
        top (action :name "(scroll! v :to :top)" :handler (fn [e] (scroll! t :to :top)))
        bottom (action :name "(scroll! v :to :bottom)" :handler (fn [e] (scroll! t :to :bottom)))
        point (action :name "(scroll! v :to [:point 50 50]"
                      :handler (fn [e] (scroll! t :to [:point 50 50])))
        rect (action :name "(scroll! v :to [:rect 100 50 20 20]"
                      :handler (fn [e] (scroll! t :to [:rect 100 50 20 20])))
        p (border-panel :center (scrollable t) 
                        :south (grid-panel :columns 2 :items [top bottom point rect]))]
    p))

(defn app-panel []
  (tabbed-panel :tabs [{:title "General" :content (general)}]))

(defn -main [& args]
  (invoke-later
    (-> (frame :title "Seesaw Scroll Demo" :size [600 :by 300] :content (app-panel))
      show!)))

(-main)

