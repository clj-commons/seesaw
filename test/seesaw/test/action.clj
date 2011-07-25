;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.action
  (:use seesaw.action)
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
    (expect (= 99 (.getValue (action :mnemonic 99) Action/MNEMONIC_KEY))))
  (it "sets the mnemonic of the action given a character"
    (expect (= (int \T) (.getValue (action :mnemonic \T) Action/MNEMONIC_KEY))))
  (it "sets the mnemonic of the action given a lower-case character"
    (expect (= (int \T) (.getValue (action :mnemonic \t) Action/MNEMONIC_KEY))))
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
    (.getValue (action :selected? true) Action/SELECTED_KEY)))

