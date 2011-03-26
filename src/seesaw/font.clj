(ns seesaw.font
  (:import (java.awt Font)))

(def ^{:private true} style-table 
  { :bold Font/BOLD 
    :plain Font/PLAIN
    :italic Font/ITALIC })

(def ^{:private true} name-table
  { :monospaced Font/MONOSPACED 
    :serif Font/SERIF
    :sans-serif Font/SANS_SERIF })

(defn font
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
    
