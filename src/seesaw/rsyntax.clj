;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Support for RSyntaxTextArea: http://fifesoft.com/rsyntaxtextarea/index.php"
      :author "Dave Ray"}
  seesaw.rsyntax
  (:require [seesaw.core :as core]
            [seesaw.util :as util]
            [seesaw.options :as options]
            [seesaw.widget-options :as widget-options]
            clojure.reflect
            clojure.string)
  (:import [org.fife.ui.rsyntaxtextarea SyntaxConstants]))

(defn- normalize-style-name [s]
  (-> (name s)
    (clojure.string/replace "_" "-")
    (clojure.string/lower-case)
    (.substring (count "SYNTAX_STYLE_"))
    keyword))

(def ^{:private true} syntax-table
  (->> (clojure.reflect/reflect SyntaxConstants)
    :members
    (map :name)
    ; there's gotta be a better way
    (map (fn [n]
           [(normalize-style-name n) (eval `(. SyntaxConstants ~n))]))
    (into {})))

(def text-area-options
  (merge
    core/text-area-options
    (options/option-map
      (options/bean-option
        [:syntax :syntax-editing-style]
        org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
        syntax-table
        nil
        (keys syntax-table)))))

(widget-options/widget-option-provider
  org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
  text-area-options)

(defn text-area
  "Create a new RSyntaxTextArea.

  In addition to normal seesaw.core/text stuff, supports the following:

    :syntax The syntax highlighting. Defaults to :none. Use
            seesaw.dev/show-options to get full list.

  See:
    (seesaw.core/text)
    http://javadoc.fifesoft.com/rsyntaxtextarea/
  "
  [& opts]
  (apply core/config! (org.fife.ui.rsyntaxtextarea.RSyntaxTextArea.) opts))