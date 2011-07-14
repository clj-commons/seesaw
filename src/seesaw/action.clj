;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for dealing with Swing Actions. Prefer (seesaw.core/action)."
      :author "Dave Ray"}
  seesaw.action
  (:use [seesaw util icon keystroke meta])
  (:import [javax.swing Action AbstractAction]))

;*******************************************************************************
; Actions

; store the handler function in a property on the action.
(def ^{:private true} action-handler-property "seesaw-action-handler")
(def ^{:private true} action-options {
  :enabled?  #(.setEnabled ^Action %1 (boolean %2))
  :selected? #(.putValue ^Action %1 Action/SELECTED_KEY (boolean %2))
  :name      #(.putValue ^Action %1 Action/NAME (str %2))
  :command   #(.putValue ^Action %1 Action/ACTION_COMMAND_KEY (str %2))
  :tip       #(.putValue ^Action %1 Action/SHORT_DESCRIPTION (str %2))
  :icon      #(.putValue ^Action %1 Action/SMALL_ICON (icon %2))
  :key       #(.putValue ^Action %1 Action/ACCELERATOR_KEY (keystroke %2))
  :mnemonic  #(.putValue ^Action %1 Action/MNEMONIC_KEY (int %2))
  :handler   #(put-meta! %1 action-handler-property %2)
})

(defn action 
  "Construct a new Action object. Supports the following properties:

    :enabled? Whether the action is enabled
    :selected? Whether the action is selected (for use with radio buttons, 
               toggle buttons, etc.
    :name      The name of the action, i.e. the text that will be displayed
               in whatever widget it's associated with
    :command   The action command key. An arbitrary string identifier associated
               with the action.
    :tip       The action's tooltip
    :icon      The action's icon. See (seesaw.core/icon)
    :key       A keystroke associated with the action. See (seesaw.keystroke/keystroke).
    :mnemonic  A character associated with the action.
    :handler   A single-argument function that performs whatever operations are
               associated with the action. The argument is a ActionEvent instance.

  Instances of action can be passed to the :action option of most buttons, menu items,
  etc.

  Actions can be later configured with the same properties above with (seesaw.core/config!).

  Returns an instance of javax.swing.Action.

  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/Action.html
  "
  [& opts]
  (let [a (proxy [AbstractAction] [] 
            (actionPerformed [e] 
              (if-let [f (get-meta this action-handler-property)] (f e))))]
    (apply-options a opts action-options)))

