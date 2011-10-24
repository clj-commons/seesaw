;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for styling apps. Prefer (seesaw.core/stylesheet) and friends."
      :author "Dave Ray"}
  seesaw.style
  (:use [seesaw.config :only [config!]]
        [seesaw.selector]))

(defn apply-stylesheet
  "ALPHA - EXPERIMENTAL AND GUARANTEED TO CHANGE

  Apply a stylesheet to a widget hierarchy. A stylesheet is simple a map where
  the keys are selectors and the values are maps from widget properties to
  values. For example,

    (apply-stylesheet frame {
      [:#foo]       { :text \"hi\" }
      [:.important] { :background :red } })

  Applying a stylesheet is a one-time operation. It does not set up any
  kind of monitoring. Thus, if you make a change to a widget that would
  affect the rules that apply to it (say, by changing its :class) you'll
  need to reapply the stylesheet.

  See:
    (seesaw.core/config!)
    (seesaw.core/select)
  "
  [root stylesheet]
  (doseq [[sel style] stylesheet]
    (apply config! (select root sel) (reduce concat style)))
  root)

