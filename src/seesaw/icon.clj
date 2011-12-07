;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for loading and creating icons."
      :author "Dave Ray"}
  seesaw.icon
  (:use [seesaw.util :only [resource resource-key? to-url]])
  (:require [clojure.java.io :as jio])
  (:import [javax.swing ImageIcon]))

;*******************************************************************************
; Icons

(defn ^javax.swing.Icon icon 
  "Loads an icon. The parameter p can be any of the following:
  
    nil              - returns nil
    javax.swing.Icon - returns the icon
    java.awt.Image   - returns an ImageIcon around the image
    java.net.URL     - Load the icon from the given URL
    an i18n keyword  - Load the icon from the resource bundle
    classpath path string  - Load the icon from the classpath
    URL string       - Load the icon from the given URL
    java.io.File     - Load the icon from the File

  This is the function used to process the :icon property on most widgets
  and windows. Thus, any of these values may be used for the :icon property.
  "
  [p]
  (cond
    (nil? p) nil 
    (instance? javax.swing.Icon p) p
    (instance? java.awt.Image p)   (ImageIcon. ^java.awt.Image p)
    (instance? java.net.URL p)     (ImageIcon. ^java.net.URL p)
    (instance? java.io.File p)     (ImageIcon. (.getAbsolutePath ^java.io.File p))
    (resource-key? p)              (icon (resource p))
    :else
      (if-let [url (jio/resource (str p))]
        (icon url)
        (if-let [url (to-url p)] 
          (ImageIcon. url)))))

