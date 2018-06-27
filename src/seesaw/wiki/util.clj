(ns seesaw.wiki.util
  (:require [seesaw.options :as opt]
            [seesaw.util :as util]
            [clojure.string :as string]
            [seesaw.event :as ev]))


(defn show-events
  "Given a class or instance, print information about all supported events.
   From there, you can look up javadocs, etc.

  Examples:

    (show-events javax.swing.JButton)
    ... lots of output ...

    (show-events (button))
    ... lots of output ...
  "
  [v]
  (doseq [{:keys [name ^Class class events]} (->> (ev/events-for v)
                                                  (sort-by :name))]
    (printf "%s [%s]%n" name (if class (.getName class) "?"))
    (doseq [e (sort events)]
      (printf "  %s%n" e))))

(defn- examples-str [examples]
  (string/join (format "%n  %24s  " "") (util/to-seq examples)))

(defn show-options
  "Given an object, print information about the options it supports. These
  are all the options you can legally pass to (seesaw.core/config) and
  friends."
  [v]
  (printf "%s%n" (.getName (class v)))
  (printf "  %24s  Notes/Examples%n" "Option")
  (printf "--%24s  --------------%n" (apply str (repeat 24 \-)))
  (doseq [{:keys [name setter examples]} (sort-by :name (vals (opt/get-option-map v)))]
    (printf "  %24s  %s%n"
            name
            (if examples (examples-str examples) ""))))
