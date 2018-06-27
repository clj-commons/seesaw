(ns seesaw.wiki.c-2-frames
  (:require [seesaw.core :as sw]
            [seesaw.wiki.util :as util]))

(def f (sw/frame :title "An example"
                 :on-close :hide
                 :content "Some Content"
                 :size [640 :by 400]
                 :visible? true))

(-> f sw/show!)

(util/show-options f)

(seesaw.core/to-root f)