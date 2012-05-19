;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.test.examples.toggle-listbox
  (:import [java.awt Dimension]
           [java.awt.image BufferedImage]
           [javax.swing ImageIcon])
  (:use seesaw.core
        seesaw.test.examples.example)
  (:require [seesaw.dnd :as dnd]))


;; I learned about the trick of first calling setSize on a Swing
;; component, in order to paint an image of it to a Graphics object
;; without displaying it on the screen, on the following web page:

;; https://forums.oracle.com/forums/thread.jspa?messageID=5697465&

(defn component-icon-image [comp pref-width pref-height]
  (let [pref-siz (Dimension. pref-width pref-height)
        bi (BufferedImage. pref-width pref-height BufferedImage/TYPE_3BYTE_BGR)
        gr (.getGraphics bi)]
    (.fillRect gr 0 0 pref-width pref-height)
    (.setSize comp pref-siz)
    (.paint comp gr)
    (ImageIcon. bi)))


(defn toggle-listbox
  "A listbox of strings that are displayed as icons that look like
toggle buttons.  This can be combined with the reorderable-listbox
example to make a reorderable list of items that look like toggle
buttons.  Note that if you get/set the current selection of the
listbox, you do so as strings or sequences of strings.  The icons are
there purely for display purposes."
  [label-strs]
  (let [buttons (map (fn [label]
                       {:label label
                        :button-sel (toggle :text label :selected? true)
                        :button-unsel (toggle :text label :selected? false)})
                     label-strs)
        ;; Make all icons the same size, the max of any individual
        ;; button's preferred size.
        max-width (apply max (map #(-> % :button-sel .getPreferredSize .width)
                                  buttons))
        max-height (apply max (map #(-> % :button-sel .getPreferredSize .height)
                                   buttons))
        label-to-icon-sel (into {}
                           (map (fn [{:keys [label button-sel]}]
                                  [label
                                   (component-icon-image button-sel
                                                         max-width max-height)])
                                buttons))
        label-to-icon-unsel (into {}
                           (map (fn [{:keys [label button-unsel]}]
                                  [label
                                   (component-icon-image button-unsel
                                                         max-width max-height)])
                                buttons))
        render-item (fn [renderer info]
                      (let [{:keys [value selected?]} info
                            m (if selected?
                                label-to-icon-sel
                                label-to-icon-unsel)]
                        (config! renderer :icon (m value) :text "")))]
    (listbox :model label-strs
             :renderer render-item)))


(defexample []
  (frame
   :title "List with strings shown as toggle buttons"
   :content
   (toggle-listbox ["Pie" "Cake" "Cookies" "Ice Cream" "Donut"])))
