;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.cells
  (:use [seesaw util]))

(defn default-list-cell-renderer 
  [& args]
  (let [opts (apply hash-map args)
        text-fn (:text opts)
        icon-fn (:icon opts)
        font-fn (:font opts)
        forground-fn (:foreground opts)]
    (proxy [javax.swing.DefaultListCellRenderer] []
      (getListCellRendererComponent [component value index selected? focus?]
        (let [info { :this this 
                     :component component 
                     :value value 
                     :index index 
                     :selected? selected? 
                     :focus? focus? }]
          (proxy-super getListCellRendererComponent component value index selected? focus?)
          (cond-doto this
            forground-fn (.setForeground (forground-fn info))
            text-fn (.setText (text-fn info))
            icon-fn (.setIcon (icon-fn info))
            font-fn (.setFont (font-fn info))))))))
      

