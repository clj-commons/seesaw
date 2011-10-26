;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.multi-dialog
  (:use [seesaw core]))

; Tiny test for showing multiple "document modal" dialogs at the same time.

(defn do-dialog [title]
  (-> (dialog 
        :title title 
        :modal? :document
        :options [(button :text "CLICK!" :listen [:action (fn [e] (return-from-dialog e title))])])
    pack! show!))

(defn make-frame [title]
  (frame
    :title title
    :content (action :name "Start Dialog" :handler (fn [e] (do-dialog title)))))


(defn -main [& args]
  (invoke-later
    (-> [(make-frame "FIRST") (make-frame "SECOND")] pack! show!)))
;(-main)

