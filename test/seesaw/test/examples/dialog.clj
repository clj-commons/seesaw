;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.dialog
  (:use [seesaw core font border util color pref mig]
        [clojure.pprint :only (cl-format)]
        seesaw.test.examples.example)
  (:require [seesaw.bind :as bind]))

(defmethod print-dup java.awt.Color [x writer]
           (binding [*print-dup* false]
             (cl-format writer "#=(java.awt.Color. ~a ~a ~a)" (.getRed x) (.getGreen x) (.getBlue x))))

(defn bound-slider
  [pref init & opts]
  (let [s (apply slider :value init opts)
        a (atom init)]
    (bind/bind (.getModel s) (bind-preference-to-atom pref a))
    (config! s :value @a)
    s))

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
    (-> (apply dialog
           (concat [:option-type :ok-cancel
                    :success-fn (fn [pane] [(selection (select (to-frame pane) [:#angle]))])]
                   common-opts)) pack! show!))

  (defn open-display-options-custom-dlg
    []
    (-> (apply dialog
            (concat [:options [(action :name "Save" :handler (fn [e] (return-from-dialog e :save)))
                                (action :name "Delete" :handler (fn [e] (return-from-dialog e :delete)))
                                (action :name "Cancel" :handler (fn [e] (return-from-dialog e nil)))]]
                    common-opts)) pack! show!))
  (defn open-display-options-remembered-dlg
    []
    (-> (dialog :id :dlg 
            ;;:options [ok-act cancel-act]
            :success-fn (fn [p]
                          (let [knn (selection (select (to-frame p) [:#knn]))] 
                            [[(selection (select (to-frame p) [:#mesh-p])) (selection (select (to-frame p) [:#plot-p]))]
                             (selection (select (to-frame p) [:#angle]))
                             (selection (select (to-frame p) [:#mode]))
                             (.getBackground (select (to-frame p) [:#colorbtn]))
                             knn]))
            :cancel-fn (fn [p] nil)
            :option-type :ok-cancel
            :content (mig-panel :items
                                [[(label :font (font :from (.getFont (javax.swing.UIManager/getDefaults) "Label.font")
                                                     :style :bold)
                                         :text "Display options for new geometry") "gaptop 10, wrap"]
                                 [:separator "growx, wrap, gaptop 10, spanx 2"]
                                 ["Generate"]
                                 [(checkbox :id :mesh-p :text "Mesh") "split"]
                                 [(checkbox :id :plot-p :text "Plot") "wrap"]
                                 ["Display mode"]
                                 [(combobox :id :mode :model ["Triangulated Mesh" "Lines"]) "wrap"]
                                 ["Angle"]
                                 [(bound-slider "LAST_ANGLE" 150 :id :angle :min 0 :max 20 :minor-tick-spacing 1 :major-tick-spacing 20 :paint-labels? true) "wrap"]
                                 ["KNN"]
                                 [(bound-slider "LAST_KNN" 150 :id :knn  :min 0 :max 300
                                          :minor-tick-spacing 10 :major-tick-spacing 100 :paint-labels? true)
                                  "wrap"] 
                                 ["Color"]
                                 [(let [lbl (label :id :colorbtn :text "      " :background (color 255 255 0)
                                         :listen [:mouse-clicked
                                                  (fn [e]
                                                    (if-let [clr (javax.swing.JColorChooser/showDialog nil "Choose a color" (.getBackground (.getSource e)))]
                                                      (config! e :background clr)))])
                                        color-atom (atom (color 255 255 0))]
                                    (bind-preference-to-atom "LAST_BACKGROUND" color-atom)
                                    (bind/bind (bind/property lbl :background) color-atom)
                                    (config! lbl :background @color-atom)
                                    lbl) "growx, wrap"]
                                 ])) pack! show!)))

(defexample []
  (frame :title "Custom Dialog Example"
         :resizable? false
         :content (vertical-panel :items [(action :name "Show Dialog with custom :success-fn" 
                                                  :handler (fn [e] (alert (str "Result = " (open-display-options-dlg)))))
                                          (action :name "Show Dialog with custom option buttons" 
                                                  :handler (fn [e] (alert (str "Result = " (open-display-options-custom-dlg)))))
                                          (action :name "Show Dialog with remembered values" 
                                                  :handler (fn [e] (alert (str "Result = " (open-display-options-remembered-dlg)))))])))
;(run :dispose)

