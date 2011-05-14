;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.action
  (:use [seesaw util icon keystroke])
  (:import [javax.swing Action AbstractAction]))

;*******************************************************************************
; Actions

; store the handler function in a property on the action.
(def ^{:private true} action-handler-property "seesaw-action-handler")
(def ^{:private true} action-options {
  :enabled?  #(.setEnabled %1 (boolean %2))
  :selected? #(.putValue %1 Action/SELECTED_KEY (boolean %2))
  :name      #(.putValue %1 Action/NAME (str %2))
  :command   #(.putValue %1 Action/ACTION_COMMAND_KEY (str %2))
  :tip       #(.putValue %1 Action/SHORT_DESCRIPTION (str %2))
  :icon      #(.putValue %1 Action/SMALL_ICON (icon %2))
  :handler   #(.putValue %1 action-handler-property %2)
  :key       #(.putValue %1 Action/ACCELERATOR_KEY (keystroke %2))
})

(defn action [& opts]
  (let [a (proxy [AbstractAction] [] 
            (actionPerformed [e] 
              (if-let [f (.getValue this action-handler-property)] (f e))))]
    (apply-options a opts action-options)))

