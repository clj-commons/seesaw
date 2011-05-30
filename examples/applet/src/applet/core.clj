(ns applet.core
  (:use [seesaw core applet])
  (:import [javax.swing JApplet]))

(defn make-content [applet]
  (border-panel
    :north "This is a Seesaw applet"
    :center (canvas :id :canvas :background "#550000")
    :south "This is a Seesaw applet"))

(defapplet
  :content make-content)

;keytool -genkey -alias seesaw -dname "cn=daveray, c=en"
;pass=seesaw
;keytool -selfcert -alias seesaw -dname "cn=daveray, c=en"
;lein uberjar
;jarsigner -storepass seesaw applet-1.0.0-SNAPSHOT-standalone.jar seesaw

