;lein deps; lein compile; export DISPLAY=:99.0; rm -Rf test/ ./lazytest.sh"  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.swingx
  (:use [seesaw.core]
        [seesaw.swingx]
        seesaw.test.examples.example)
  (:require [seesaw.bind :as b]))

; A demo program that shows some of the components in Seesaw's SwingX support.
(defn demo [title desc content]
  (border-panel
    :id :demo
    :hgap 5 :vgap 5 :border 5
    :north (header :title title :description (str "<html>" desc "</html>"))
    :center content))

(def demos
  (delay {:table-x (demo "(seesaw.swingx/table-x)" "Like (table), but slightly less sucky."
                  (scrollable
                    (table-x :model [:columns [:name :file :line]
                                     :rows (map (comp meta val) (ns-publics 'seesaw.core))]
                        :horizontal-scroll-enabled? true)))
   :label-x (demo "(seesaw.swingx/label-x)" "Like (label), but supports line wrapping, and painters. <i>It also support rotation, but is buggy</i>. :("
                  (vertical-panel :items [
                    (label-x :wrap-lines? true
                             :text (str "<html>"
                                        (apply str (repeat 10 "This is a <i>label-x</i> with <b>a lot</b> of content "))
                                        "</html>"))]))
   :busy-label (demo "(seesaw.swingx/busy-label" "A label with a busy indicator, enabled with the :busy? option"
                     (border-panel
                       :center (busy-label :text "Processing ..."
                                           :busy? true)))

   :hyperlink (demo "(seesaw.swingx/hyperlink)" "A button that looks like a hyperlink. If given the :uri option, opens in browser. Can have an :icon if you like."
                    (vertical-panel :items [
                      (hyperlink :uri "http://github.com/daveray/seesaw" :text "This link opens Seesaw's github page in a browser")
                      :separator
                      (hyperlink :action (action :name "This link acts like a button"
                                                 :handler (fn [e] (alert "You clicked the link"))
                                                 :tip "This is a tooltip for the link"))
                      :separator
                      (hyperlink :text "This link has an icon"
                                 :icon "seesaw/test/examples/rss.gif")]))

   :task-pane (demo "(seesaw.swingx/task-pane) and (seesaw.swingx/task-pane-container)"
                    "A collapsible task pane. Each pane can have an icon, title, etc, etc"
                    (task-pane-container
                      :items [(task-pane :title "First"
                                :actions [(action :name "HI")
                                          (action :name "BYE")])
                              (task-pane :title "Second"
                                :actions [(action :name "HI")
                                          (action :name "BYE")])
                              (task-pane :title "Third" :special? true :collapsed? true
                                :items [(button :text "YEP")])]))

  :color-selector (demo "(seesaw.swingx/color-selection-button)"
                        "A button that shows a color selection and brings up a color chooser when clicked."
                        (let [b (color-selection-button)
                              l (label :text "Color button selection is bound to my background color.")]
                          (b/bind (b/selection b) (b/property l :background))
                          (selection! b :green)
                          (border-panel
                            :north (flow-panel :items ["Select a color: " b])
                            :center l)))

   :listbox-x (demo "(seesaw.swingx/listbox-x)"
                    "A drop in replacement for (seesaw.core/listbox) with support for sorting, highlighters, etc"
                    (border-panel
                      :hgap 5 :vgap 5
                      :north "In the listbox below, striping and rollover highlighting is added"
                      :center (scrollable (listbox-x
                                            :model (ns-publics 'seesaw.core)
                                            :sort-order :ascending
                                            :highlighters [(hl-simple-striping)
                                                           ((hl-color :background :darkgreen) :rollover-row)]))))

   :titled-panel (demo "(seesaw.swingx/titled-panel)"
                       "A panel with a title and content"
                       (titled-panel
                         :content (label-x :wrap-lines? true
                                           :text "This is the content of the titled panel. The title can also have decorations, etc." )
                         :title "The title of the panel"))}))

(defn make-ui []
  (frame
    :title "Seesaw SwingX Example"
    :size [640 :by 480]
    :content
    (border-panel
      :hgap 5 :vgap 5 :border 5
      :north  (label-x :wrap-lines? true
                       :text "This example shows some of the various widgets provided by SwingX. See seesaw.swingx because there is more than what's shown here." )
      :center (left-right-split
                (scrollable
                  (listbox-x
                    :id :chooser
                    :model (->> (keys @demos) sort)
                    :highlighters [(hl-simple-striping)]))
                (border-panel :id :demo-container
                              :center (:label-x @demos))
                :divider-location 1/4))))

(defn add-behaviors [root]
  (let [chooser   (select root [:#chooser])
        container (select root [:#demo-container])]
    (listen chooser :selection
      (fn [e]
        (replace! container
                  (select container [:#demo])
                  (@demos (selection chooser)) ))))
  root)

(defexample []
  (-> (make-ui) add-behaviors))

;(run :dispose)

