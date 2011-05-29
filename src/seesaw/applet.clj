(ns seesaw.applet
  (:use [seesaw core])
  (:import [javax.swing JApplet]))

          ;~(when content
            ;`(.setContentPane ~'applet (seesaw.core/border-panel :center ~content)))))
(defmacro defapplet 
  [& {:keys [name init start stop content] 
      :or {init  '(fn [applet]) 
           start '(fn [applet]) 
           stop  '(fn [applet])
           content '(fn [applet] (seesaw.core/label "A Seesaw Applet"))}}]
  (let [applet (gensym "applet")]
    `(do
      (gen-class
        :name ~name
        :extends javax.swing.JApplet
        :prefix "-seesaw-applet-")

      (defn -seesaw-applet-init [#^JApplet ~applet]
        (seesaw.core/invoke-later 
          (do
            (~init ~applet)
            (doto ~applet
              (.setLayout (java.awt.BorderLayout.))
              (.add (~content ~applet) java.awt.BorderLayout/CENTER)
              (.. getContentPane revalidate repaint)))))

      (defn -seesaw-applet-start [#^JApplet ~applet]
        (seesaw.core/invoke-later 
          (~start ~applet)))

      (defn -seesaw-applet-stop [#^JApplet ~applet]
        (seesaw.core/invoke-later 
          (~stop ~applet))))))

(defapplet 
  :name seesaw.applet 
  :init #(seesaw.core/alert (str "INIT!" (.getCodeBase %)))
  ;:start #(seesaw.core/alert (str "START"))
  :stop (fn [applet] (seesaw.core/alert "STOP!"))
  :content (fn [applet] (seesaw.core/vertical-panel :items ["A" "B" "C" "D"]))
  )

;keytool -genkey -alias seesaw -dname "cn=daveray, c=en"
;pass=seesaw
;keytool -selfcert -alias seesaw -dname "cn=daveray, c=en"
;lein uberjar
;jarsigner clojure.jar seesaw

