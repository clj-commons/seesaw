;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for mapping key strokes to actions."
      :author "Dave Ray"}
  seesaw.keymap
  (:use [seesaw.util :only [illegal-argument]]
        [seesaw.keystroke :only [keystroke]]
        [seesaw.action :only [action]]
        [seesaw.to-widget :only [to-widget*]]))

(defn- ^javax.swing.Action to-action [act]
  (cond
    (nil? act) nil

    (instance? javax.swing.Action act) act

    (instance? javax.swing.AbstractButton act)
      (let [^javax.swing.AbstractButton b act]
        (action :handler (fn [_] (.doClick b))))

    (fn? act) 
      (action :handler act)
    :else (illegal-argument "Don't know how to make key-map action from '%s'" act)))

(defn- ^javax.swing.JComponent to-target [target]
  (cond
    (instance? javax.swing.JComponent target) target
    (instance? javax.swing.JFrame target) (.getRootPane target)
    :else (illegal-argument "Don't know how to map keys on '%s'" target)))

(def ^{:private true} scope-table
  { :descendants javax.swing.JComponent/WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
    :self        javax.swing.JComponent/WHEN_FOCUSED
    :global      javax.swing.JComponent/WHEN_IN_FOCUSED_WINDOW })

(def ^{:private true} default-scope (:descendants scope-table))

(defn map-key
  "Install a key mapping on a widget. 
  
  Key mappings are hopelessly entwined with keyboard focus and the widget 
  hierarchy. When a key is pressed in a widget with focus, each widget up
  the hierarchy gets a chance to handle it. There three 'scopes' with
  which a mapping may be registered:
  
    :self 

      The mapping only handles key presses when the widget itself has
      the keyboard focus. Use this, for example, to install custom
      key mappings in a text box.
  
    :descendants
  
      The mapping handles key presses when the widget itself or any
      of its descendants has keyboard focus. 
  
    :global
  
      The mapping handles key presses as long as the top-level window
      containing the widget is active. This is what's used for menu
      shortcuts and should be used for other app-wide mappings.
  
  Given this, each mapping is installed on a particular widget along
  with the desired keystroke and action to perform. The keystroke can
  be any valid argument to (seesaw.keystroke/keystroke). The action
  can be one of the following:
  
    * A javax.swing.Action. See (seesaw.core/action)
    * A single-argument function. An action will automatically be
      created around it.
    * A button, menu, menuitem, or other button-y thing. An action
      that programmatically clicks the button will be created.
    * nil to disable or remove a mapping 

  target may be a widget, frame, or something convertible through to-widget.

  Returns a function that removes the key mapping.

  Examples:

    ; In frame f, key \"K\" clicks button b
    (map-key f \"K\" b)

    ; In text box t, map ctrl+enter to a function
    (map-key t \"control ENTER\"
      (fn [e] (alert e \"You pressed ctrl+enter!\")))

  See:
    (seesaw.keystroke/keystroke)
    http://download.oracle.com/javase/tutorial/uiswing/misc/keybinding.html
  "
  [target key act & {:keys [scope id] :as opts}]
  (let [target (to-target (to-widget* target))
        scope  (scope-table scope default-scope)
        im     (.getInputMap target scope)
        am     (.getActionMap target)
        act    (to-action act)
        id     (or id act)
        ks     (keystroke key)]
    (.put im ks id)
    (.put am id act)
    (fn []
      (.remove im ks)
      (.remove am id))))

