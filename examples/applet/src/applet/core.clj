(ns applet.core
  (:use [seesaw core applet])
  (:import [javax.swing JApplet]))

(defn make-content [applet]
  (border-panel
    :north "This is a Seesaw applet"
    :center (canvas :id :canvas :background "#550000")
    :south "Some kind of status bar"))

(defapplet
  :name applet.core
  ; :init a function call at init
  ; :start a function called when the applet is started
  ; :stop a function called when the applet is stopped
  ; A function called the returns the root panel to display
  :content make-content)

;keytool -genkey -alias seesaw -dname "cn=daveray, c=en"
;pass=seesaw
;keytool -selfcert -alias seesaw -dname "cn=daveray, c=en"
;lein uberjar
;jarsigner -storepass seesaw applet-1.0.0-SNAPSHOT-standalone.jar seesaw

