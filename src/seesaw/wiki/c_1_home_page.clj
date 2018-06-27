(ns seesaw.wiki.c-1-home-page
  (:require [seesaw.core :as saw]
            [seesaw.event :as ev]
            [seesaw.options :as opt]
            [seesaw.wiki.util :as util]
            [clojure.string :as string]))

(util/show-options (saw/label))

(util/show-events (saw/label))

(util/show-events (saw/button))