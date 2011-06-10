;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Macros and functions that make creating an applet with Seesaw a
            little less painful."
      :author "Dave Ray"}
  seesaw.applet
  (:use [seesaw core])
  (:import [javax.swing JApplet]))

(defmacro defapplet
  "Define an applet. This macro does all the gen-class business and takes maps
  applet lifetime methods to callback functions automatically. Supports the
  following options:
  
    :name    The name of the generated class. Defaults to the current namespace.

    :init    Function called when the applet is first loaded. Takes a single
             JApplet argument. This function is called from the UI thread.

    :start   Function called when the applet is started. Takes a single JApplet
             argument. This function is called from the UI thread.

    :stop    Function called when the applet is stopped. Takes a single JApplet
             argument. This function is called from the UI thread.

    :content Function called after :init which should return the content of
             the applet, for example some kind of panel. It's added to the center
             of a border pane so it will be resized with the applet.
 
  Note that the namespace containing a call to (defapplet) must be compiled. In
  Leiningen, this is easiest to do by adding an :aot option to project.clj:

    :aot [namespace.with.defapplet]

  After that, use \"lein uberjar\" to build a jar with everything.

  Since Seesaw is currently reflection heavy, the resulting jar must be signed:

    $ keytool -genkey -alias seesaw -dname \"cn=company, c=en\"
    $ keytool -selfcert -alias seesaw -dname \"cn=company, c=en\"
    $ lein uberjar
    $ jarsigner name-of-project-X.X.X-SNAPSHOT-standalone.jar seesaw

  Then refer to it from your webpage like this:

    <applet archive=\"name-of-project-X.X.X-standalone.jar\" 
            code=\"namespace/with/defapplet.class\" 
            width=\"200\" 
            height=\"200\">

  Examples:

    See examples/applet project.

  See:
    http://download.oracle.com/javase/7/docs/api/javax/swing/JApplet.html
    http://download.oracle.com/javase/tutorial/uiswing/components/applet.html
    http://download.oracle.com/javase/tutorial/deployment/applet/index.html
  "
  [& {:keys [name init start stop content]
      :or {init    '(fn [applet]) 
           start   '(fn [applet]) 
           stop    '(fn [applet])
           content '(fn [applet] (seesaw.core/label "A Seesaw Applet"))}}]
  (let [applet (gensym "applet")]
    `(do
      (gen-class
        :name ~(or name (ns-name *ns*))
        :extends javax.swing.JApplet
        :prefix "-seesaw-applet-")

      (defn ~'-seesaw-applet-init [#^javax.swing.JApplet ~applet]
        (seesaw.core/invoke-later 
          (do
            (~init ~applet)
            (doto ~applet
              (.setLayout (java.awt.BorderLayout.))
              (.add (seesaw.core/to-widget (~content ~applet)) java.awt.BorderLayout/CENTER)
              (.. getContentPane revalidate repaint)))))

      (defn ~'-seesaw-applet-start [#^JApplet ~applet]
        (seesaw.core/invoke-later 
          (~start ~applet)))

      (defn ~'-seesaw-applet-stop [#^JApplet ~applet]
        (seesaw.core/invoke-later 
          (~stop ~applet))))))

