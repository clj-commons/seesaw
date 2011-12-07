;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.action
  (:use [seesaw.action]
        [seesaw.core :only [config]]
        [seesaw.keystroke :only [keystroke]])
  (:use [lazytest.describe :only (describe it testing)]
        [lazytest.expect :only (expect)])
  (:import [javax.swing Action]))

(describe action
  (it "sets the name, tooltip, and command"
    (let [a (action :name "Test" :tip "This is a tip" :command "Go!")]
      (expect (instance? Action a))
      (expect (.isEnabled a))
      (expect (= "Test" (.getValue a Action/NAME)))
      (expect (= "Go!" (.getValue a Action/ACTION_COMMAND_KEY)))
      (expect (not (.getValue a Action/SELECTED_KEY)))
      (expect (= "This is a tip" (.getValue a Action/SHORT_DESCRIPTION)))))
  (it "sets the mnemonic of the action given an integer key code"
    (let [m (.getValue (action :mnemonic 99) Action/MNEMONIC_KEY)] 
      ; For Clojure 1.3, ensure that it's an Integer in there and not a Long
      (expect (instance? java.lang.Integer m))
      (expect (= 99 m))))
  (it "sets the mnemonic of the action given a character"
    (let [m (.getValue (action :mnemonic \T) Action/MNEMONIC_KEY)]
      ; For Clojure 1.3, ensure that it's an Integer in there and not a Long
      (expect (instance? java.lang.Integer m))
      (expect (= (int \T) m))))
  (it "sets the mnemonic of the action given a lower-case character"
    (let [m (.getValue (action :mnemonic \t) Action/MNEMONIC_KEY)] 
      ; For Clojure 1.3, ensure that it's an Integer in there and not a Long
      (expect (instance? java.lang.Integer m))
      (expect (= (int \T) m))))
  (it "calls the handler when actionPerformed is called"
    (let [called (atom false)
          f (fn [e] (reset! called true))
          a (action :handler f)]
      (.actionPerformed a nil)
      (expect @called)))
  (it "does nothing when actionPerformed is called and no handler is installed"
    (let [a (action)]
      (.actionPerformed a nil)
      ; Just making sure no exception was thrown
      true))
  (it "handles the :key option"
    (let [a (action :key "menu T")
          ks (.getValue a Action/ACCELERATOR_KEY)]
      (expect (not (nil? ks)))
      (expect (instance? javax.swing.KeyStroke ks))))
  (it "handles the :enabled? option"
    (not (.isEnabled (action :enabled? false))))
  (it "handles the :selected? option"
    (.getValue (action :selected? true) Action/SELECTED_KEY))

  (it "loads resources by convention with :resource option"
    (let [a (action :resource ::my-action)]
      (expect (instance? javax.swing.Icon (config a :icon)))
      (expect (= (int \X) (config a :mnemonic)))
      (expect (= "A command" (config a :command)))
      (expect (= "A name" (config a :name)))
      (expect (= "A tip" (config a :tip)))
      (expect (= (keystroke "ctrl C") (config a :key)))))

  (it "loads :icon from a resource"
    (expect (instance? javax.swing.Icon (config (action :icon ::my-action.icon) :icon))))
  (it "loads :mnemonic from a resource"
    (expect (= (int \X) (config (action :mnemonic ::my-action.mnemonic) :mnemonic))))
  (it "loads :command from a resource"
    (expect (= "A command" (config (action :command ::my-action.command) :command))))
  (it "loads :name from a resource"
    (expect (= "A name" (config (action :name ::my-action.name) :name))))
  (it "loads :key from a resource"
    (expect (= (keystroke "ctrl C") (config (action :key ::my-action.key) :key))))
  (it "loads :tip from a resource"
    (expect (= "A tip" (config (action :tip ::my-action.tip) :tip)))))

