;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.examples.j18n
  (use [seesaw.core])
  (require [j18n.core]))

; Declare a bundle. Will look on classpath for seesaw/examples/j18n.properties
; or a locale-specific file. Don't forget to call (j18n.core/init-bundles!)
; in main!
(j18n.core/defbundle bundle)

(defn -main [& args]
  ; Initialize j18n bundles
  (j18n.core/init-bundles!)
  (let [a (action 
            ; Use a set of properties for the action, all with prefix "action"
            :resource ::my-action
            :handler (fn [_] 
                       ; Use translate directly to lookup values in the bundle
                       (alert (j18n.core/translate ::my-action.click-message))))] 
    (->
      (frame 
        ; Most Seesaw properties (title, text, icon) will look in the resource
        ; bundle when a namespace-qualified keyword is given
        :title ::title
        :menubar (menubar :items [(menu 
                                    :text  ::menu.tools.text
                                    :items [a])])
        :content (vertical-panel
                  :items [(button :text ::button.text
                                  :foreground ::button.foreground
                                  :font ::button.font
                                  :icon ::button.icon)
                          a]))
      pack!
      show!)))

;(-main)
