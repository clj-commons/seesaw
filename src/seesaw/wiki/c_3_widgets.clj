(ns seesaw.wiki.c-3-widgets
  (:require [seesaw.core :as sw]
            [seesaw.options :as op]
            [seesaw.wiki.util :as util]
            [seesaw.rsyntax :as rs]
            [clojure.string :as str]))

(def size (op/satom [150 :by 300]))
(def title (op/satom "Title"))

(swap! size (fn [a] [600 :by 600]))
(swap! title (fn [_] "asdbsahd"))

(def ff (sw/frame :title title
                  :on-close :hide
                  :content (sw/scrollable (rs/text-area :syntax :clojure
                                                        :background "#F5EEDF"
                                                        :selection-color :aliceblue))
                  :size size
                  :visible? true))

(sw/frame :title "An example"
          :on-close :hide
          :content (java.net.URL. "https://github.com/daveray/seesaw/wiki/Widgets")
          :size [640 :by 400]
          :visible? true)

(util/show-options (rs/text-area :syntax :clojure))


(sw/button :id :the-button :text "Push me")

(sw/select root [:#the-button])


(def kkk (op/satom {}))

(swap! kkk assoc-in [:a :b :c] 1)

(def file (op/satom "File"))

(reset! file "File")

(def items (op/satom [(sw/menu :text "File" :items [])
                      (sw/menu :text "Edit" :items [])
                      (sw/menu :text "Exit" :items [])]))

(sw/menubar :items
            [(sw/menu :text "File" :items [])
             (sw/menu :text "Edit" :items [])
             (sw/menu :text "Exit" :items [])])

(def oo (op/satom {:title {:name "Menu" :age "12"}}))

(reset! oo {:title {:name "aaa22as34" :age "15"}})

(sw/frame :title (op/get-k oo :title :age)
          :size [640 :by 400]
          :visible? true)


({:a 1} :a)
[1 2 3]
#{1 2 3}
'(1 2 3 4)

(defn my-add
  [x y]
  (+ x y))

(my-add 1 1)