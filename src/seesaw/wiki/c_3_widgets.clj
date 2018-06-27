(ns seesaw.wiki.c-3-widgets
  (:require [seesaw.core :as sw]
            [seesaw.options :as op]
            [seesaw.wiki.util :as util]
            [clojure.string :as str]))

(comment
  (seesaw.core/make-widget))

(def p (sw/vertical-panel :items ["This" "is" "a" "vertical" "stack of" "JLabels"]))

(def fl (let [choose (fn [e] (sw/alert "I should open a file chooser"))]
          (sw/flow-panel
            :items ["File" [:fill-h 5]
                    (sw/text (System/getProperty "user.dir")) [:fill-h 5]
                    (sw/action :handler choose :name "...")])))

(def title (op/satom "Ertus"))

(vals (get @op/satoms title))

(swap! title str/lower-case)

(reset! title "Hey")


(def size (op/satom {:as 12}))

(swap! size (fn [a] [150 :by 300]))

(sw/frame :title "An example"
          :on-close :hide
          :content title
          :size size
          :visible? true)

(sw/frame :title "An example"
          :on-close :hide
          :content (java.net.URL. "https://github.com/daveray/seesaw/wiki/Widgets")
          :size [640 :by 400]
          :visible? true)

(util/show-options (sw/label))


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

(def oo (op/satom {:title {:name "Menu"}}))

(reset! oo {:title {:name "Keremcik aa"}})

(sw/frame :title (op/get-k oo :title )
          :size [640 :by 400]
          :visible? true)