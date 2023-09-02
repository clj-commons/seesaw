;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns radiance.core
  (:use [seesaw.core])
  (:import org.pushingpixels.radiance.theming.api.RadianceThemingCortex$GlobalScope)
  (:gen-class))

(defn laf-selector []
  (horizontal-panel
    :items ["Radiance skin: "
            (combobox
              :model    (vals (RadianceThemingCortex$GlobalScope/getAllSkins))
              :renderer (fn [this {:keys [value]}]
                          (text! this (.getClassName value)))
              :listen   [:selection (fn [e]
                                      ; Invoke later because CB doens't like changing L&F while
                                      ; it's doing stuff.
                                      (invoke-later
                                        (-> e
                                          selection
                                          .getClassName
                                          RadianceThemingCortex$GlobalScope/setSkin)))])]))

(def notes " This example shows the available Radiance skins. Substance
is a set of improved look and feels for Swing. To use it in a project,
you'll need to add a dep to your Leiningen project:

       [org.pushing-pixels/radiance-common  \"6.5.0\"]
       [org.pushing-pixels/radiance-theming \"6.5.0\"]

In this example, the full class name of the current skin is shown the
in the combobox above. For your own apps you could either use a
selector like this example, or, more likely, set a default initial
skin in one of the following ways:

    Start your VM with -Dswing.defaultlaf=<class-name>

    Call (javax.swing.UIManager/setLookAndFeel \"<class-name>\")
    do this *after* (seesaw.core/native!) since that sets the L&F.

See https://github.com/kirill-grouchnikov/radiance
for more info. There you'll also find much more info about the
skins along with much less crappy looking demos.")

(defn -main [& args]
  (invoke-later
    (->
      (frame
        :title "Seesaw kirill-grouchnikov/radiance Example"
        :on-close :exit
        :content (vertical-panel
                   :items [(laf-selector)
                           (text :multi-line? true :text notes :border 5)
                           :separator
                           (label :text "A Label")
                           (button :text "A Button")
                           (checkbox :text "A checkbox")
                           (combobox :model ["A combobox" "more" "items"])
                           (horizontal-panel
                             :border "Some radio buttons"
                             :items (map (partial radio :text)
                                         ["First" "Second" "Third"]))
                           (scrollable (listbox :model (range 100)))]))
      pack!
      show!)))
