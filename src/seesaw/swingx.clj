;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "SwingX integration. Unfortunately, SwingX is hosted on java.net which means
           it looks abandoned most of the time. Downloads are here 
           http://java.net/downloads/swingx/releases/1.6/"
      :author "Dave Ray"}
  seesaw.swingx
  (:use [seesaw.util :only [to-uri]]
        [seesaw.core :only [construct button-options]]
        [seesaw.options :only [option-map bean-option apply-options]]))

(def hyperlink-options
  (merge
    button-options
    (option-map
      (bean-option [:uri :URI] org.jdesktop.swingx.JXHyperlink to-uri))))

(defn hyperlink 
  "Constuct an org.jdesktop.swingx.JXHyperlink which is a button that looks like
  a link and opens its URI in the system browser. In addition to all the options of
  a button, supports:
  
    :uri A string, java.net.URL, or java.net.URI with the URI to open
 
  Examples:

    (hyperlink :text \"Click Me\" :uri \"http://google.com\")

  See:
    (seesaw.core/button)
    (seesaw.core/button-options)
  "
  [& args]
  (apply-options (construct org.jdesktop.swingx.JXHyperlink args) args hyperlink-options))

