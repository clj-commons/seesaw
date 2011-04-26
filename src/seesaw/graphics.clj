;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.graphics
  (:import [java.awt Graphics2D RenderingHints]
           [java.awt.image BufferedImage]))

(defn anti-alias 
  "Enable anti-aliasing on the given Graphics2D object"
  [g2d]
  (.setRenderingHint g2d RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON))

(defn buffered-image 
  ([width height] (buffered-image width height BufferedImage/TYPE_INT_ARGB))
  ([width height t] (BufferedImage. width height t)))

(defmacro push 
  "Push a Graphics2D context (Graphics2d/create) and automatically dispose it.
  
  For example, in a paint handler:
  
    (fn [c g2d]
      (.setColor g2d java.awt.Color/RED)
      (.drawString g2d \"This string is RED\" 0 20)
      (push g2d
        (.setColor g2d java.awt.Color/BLUE)
        (.drawString g2d \"This string is BLUE\" 0 40))
      (.drawString g2d \"This string is RED again\" 0 60))
  "
  [g2d & forms]
  `(let [~g2d (. ~g2d create)]
     (try 
       ~@forms
       (finally
         (. ~g2d dispose)))))

  

