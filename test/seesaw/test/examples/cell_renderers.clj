(ns seesaw.test.examples.cell-renderers
  (:use [seesaw core font]
        seesaw.test.examples.example))

(def even-font (font "ARIAL-ITALIC-18"))
(def odd-font (font "ARIAL--18"))

; Define a render function. The first arg is the renderer (an instance of
; DefaultListCellRenderer). The second holds the state of the cell we're
; rendering. We use :value to get the model value of the cell. Note
; that renderer is just a JLabel, so we can call config! on it and set
; properties in the normal way.
(defn render-fn [renderer info]
  (let [v (:value info)]
    (apply config! renderer 
      (if (even? v) 
        [:text (str v " is even") :font even-font :foreground "#000033"]
        [:text (str v " is odd")  :font odd-font  :foreground "#aaaaee"]))))

(defexample []
  (frame :title   "Cell Renderer Example"
         :content (scrollable 
                    (listbox :model    (range 5 25) 
                             :renderer render-fn)))) 

;(run :dispose)

