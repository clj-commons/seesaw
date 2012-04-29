(ns window-builder.core
  (:use [seesaw.core])
  (:require [seesaw.selector :as selector]))

; This is the interesting part. Note that in MyPanel.java, the widgets we're
; interested in have their name set with setName().
(defn identify
  "Given a root widget, find all the named widgets and set their Seesaw :id
   so they can play nicely with select and everything."
  [root]
  (doseq [w (select root [:*])]
    (if-let [n (.getName w)]
      (selector/id-of! w (keyword n))))
  root)

(def states ["CA", "GA", "WA"])

(def defaults 
  { :first-name "Laura"
    :last-name "Palmer"
    :street "123 Main St."
    :city "Twin Peaks"
    :zip "12345"
    :state "WA" })

; A helper to create an instance of the form, annotate it for Seesaw and do
; some other initialization.
(defn my-form 
  []
  (let [form (identify (window_builder.MyForm.))]
    ; initialize the state combobox
    (config! (select form [:#state]) :model states)
    form))

; Now we just create the panel, initialize it to the defaults above with 
; seesaw.core/value! and show it in a dialog. Note how, besides setting the
; names of the widgets, the code in MyForm.java is strictly for layout. All
; behavior, etc is done in Clojure.
(defn -main [& args]
  (invoke-later
    (let [form  (value! (my-form) defaults)
          result (-> (dialog :content form :option-type :ok-cancel) pack! show!)]
      (if (= :success result)
        (println "User entered: " (value form))
        (println "User canceled")))))

