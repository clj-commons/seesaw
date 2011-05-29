(ns seesaw.applet
  (:use [seesaw core])
  (:import [javax.swing JApplet]))

(defmacro defapplet
  [& {:keys [name init start stop content]
      :or {init    '(fn [applet]) 
           start   '(fn [applet]) 
           stop    '(fn [applet])
           content '(fn [applet] (seesaw.core/label "A Seesaw Applet"))}}]
  (let [applet (gensym "applet")]
    `(do
      (gen-class
        :name ~name
        :extends javax.swing.JApplet
        :prefix "-seesaw-applet-")

      (defn ~'-seesaw-applet-init [#^javax.swing.JApplet ~applet]
        (seesaw.core/invoke-later 
          (do
            (~init ~applet)
            (doto ~applet
              (.setLayout (java.awt.BorderLayout.))
              (.add (~content ~applet) java.awt.BorderLayout/CENTER)
              (.. getContentPane revalidate repaint))
            )))

      (defn ~'-seesaw-applet-start [#^JApplet ~applet]
        (seesaw.core/invoke-later 
          (~start ~applet)))

      (defn ~'-seesaw-applet-stop [#^JApplet ~applet]
        (seesaw.core/invoke-later 
          (~stop ~applet)))
       )
    )
   )

