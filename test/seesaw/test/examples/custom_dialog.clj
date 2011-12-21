;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.custom-dialog
  (:use [seesaw core font border mig]
        seesaw.test.examples.example))

(defn open-more-options-dlg
  []
  (let [ok-act     (action :name "Ok" :handler (fn [e] (return-from-dialog e "OK")))
        cancel-act (action :name "Cancel" :handler (fn [e] (return-from-dialog e "Cancel")))]
    (-> (custom-dialog 
          :modal? true
          :title "More Options"
          :content (flow-panel :items [ok-act cancel-act]))
      pack! 
      show!)))

(defn open-display-options-dlg
  []
  (let [ok-act (action 
                 :name "Ok"
                 :handler (fn [e] (return-from-dialog e (value (to-frame e)))))
        cancel-act (action :name "Cancel"
                           :handler (fn [e] (return-from-dialog e nil)))
        more-act (action :name "More ..."
                         :handler (fn [e] (alert (str "More Result = " (open-more-options-dlg)))))]
    (-> (custom-dialog 
          :title  "Display Options"
          :modal? true
          :resizable? false
          :content (mig-panel 
                     :border (line-border)
                     :items [[(label :font (font :from (default-font "Label.font") :style :bold)
                                     :text "Display options for new geometry") 
                               "gaptop 10, wrap"]
 
                             [:separator "growx, wrap, gaptop 10, spanx 2"]
 
                             ["Display mode:"]
 
                             [(combobox :id :mode 
                                         :model ["Triangulated Mesh" "Lines"]) 
                               "wrap"]
 
                             ["Angle"]
 
                             [(slider :id :angle 
                                       :min 0 :max 20 
                                       :minor-tick-spacing 1 :major-tick-spacing 20 
                                       :paint-labels? true)
                               "wrap"]
 
                             [(flow-panel :align :right :items [more-act ok-act cancel-act]) 
                               "spanx 2" "alignx right"]])) 
      pack! 
      show!)))

(defexample []
  (frame :title "Custom Dialog Example"
         :content (action :name "Show Dialog" 
                          :handler (fn [e] 
                                     (alert (str "Result = " (open-display-options-dlg)))))))

;(run :dispose)

