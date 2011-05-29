;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.font
  (:use [seesaw util])
  (:import [java.awt Font]))

(def ^{:private true} style-table (constant-map Font :bold :plain :italic))

(def ^{:private true} name-table (constant-map Font :monospaced :serif :sans-serif))

(defn
  "Create and return a Font.

      (font name)
      (font ... options ...)

  Options are:

    :name   The name of the font. Besides string values, also possible are 
            any of :monospaced, :serif, :sans-serif.
    :style  The style. One of :bold, :plain, :italic. Default: :plain.
    :size   The size of the font. Default: 12.
    :from   A Font from which to derive the new Font.

   Returns a java.awt.Font instance.
    "
  font
  [& args]
  (if (= 1 (count args))
    (Font/decode (str (first args)))
    (let [{:keys [style size from] :as opts} args
          font-name (:name opts)
          font-style (get style-table (or style :plain))
          font-size (or size 12)]
      (if from
        (let [derived-style (if style font-style (.getStyle from))
              derived-size (if size font-size (.getSize from))]
          (.deriveFont from derived-style (float derived-size)))
        (Font. (get name-table font-name font-name) font-style font-size)))))

(defn to-font
  [f]
  (cond
    (instance? Font f) f
    (map? f) (apply font (flatten (seq f)))
    true (font f))) 
    
