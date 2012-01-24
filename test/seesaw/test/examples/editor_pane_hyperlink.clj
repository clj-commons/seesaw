;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.editor-pane-hyperlink
  (:use seesaw.core
        seesaw.test.examples.example)
  (:import javax.swing.event.HyperlinkEvent
           javax.swing.event.HyperlinkEvent$EventType))

(defn make-editor-pane
  []
  (editor-pane
    :id :editor
    :content-type "text/html"
    :editable? false
    :text "<html>
          You can click a link and get an event:
          <ul>
          <li><a href='apple'>Apple</a></li>
          <li><a href='banana'>Banana</a></li>
          <li><a href='chocolate'>Chocolate</a></li>
          <li><a href='durian'>Durian</a></li>
          <li><a href='egg'>Egg</a></li>
          </ul>
          </html>"))

(defn add-behaviors
  [root]
  (let [editor (select root [:#editor])]
    (listen editor :hyperlink
      (fn [e]
        (when (= HyperlinkEvent$EventType/ACTIVATED (.getEventType e))
          (alert e (str "Clicked: " (.getDescription e)))))))
  root)

(defexample []
  (->
    (frame
      :title "Editor Pane Hyperlinks"
      :content (make-editor-pane))
    add-behaviors))

;(run :dispose)


