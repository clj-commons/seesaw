;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.option-pane
  (:use [seesaw core font border]))

(let [common-opts
      [:content (mig-panel :items [[(label :font (font :from (default-font "Label.font")
                                                       :style :bold)
                                           :text "Display options for new geometry") "gaptop 10, wrap"]
                                   [:separator "growx, wrap, gaptop 10, spanx 2"]
                                   ["Display mode:"]
                                   [(combobox :id :mode :model ["Triangulated Mesh" "Lines"]) "wrap"]
                                   ["Angle"]
                                   [(slider :id :angle :min 0 :max 20 :minor-tick-spacing 1 :major-tick-spacing 20 :paint-labels? true) "wrap"]])
       :resizable? false
       :title "Display Options"]]
  (defn open-display-options-dlg
    []
    (apply option-pane
           (concat [:option-type :ok-cancel
                    :success-fn (fn [pane] [(selection (select (to-frame pane) [:#angle]))])]
                   common-opts)))

  (defn open-display-options-custom-dlg
    []
    (apply option-pane
           (concat [:options [(action :name "Save" :handler (fn [e] (return-from-dialog :save)))
                              (action :name "Delete" :handler (fn [e] (return-from-dialog :delete)))
                              (action :name "Cancel" :handler (fn [e] (return-from-dialog nil)))]]
                   common-opts))))

(defn -main [& args]
  (invoke-later
    (frame :title "Custom Dialog Example"
           :resizable? false
           :content (vertical-panel :items [(action :name "Show Dialog with custom :success-fn" 
                                                    :handler (fn [e] (alert (str "Result = " (open-display-options-dlg)))))
                                            (action :name "Show Dialog with custom option buttons" 
                                                    :handler (fn [e] (alert (str "Result = " (open-display-options-custom-dlg)))))]))))
;(-main)
