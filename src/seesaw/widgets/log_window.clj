;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.widgets.log-window
  (:use [seesaw.core]
        [seesaw.keymap :only [map-key]]
        [seesaw.invoke :only [signaller]]
        [seesaw.options :only [apply-options option-map default-option]]
        [seesaw.widget-options :only [WidgetOptionProvider]]))

(defprotocol LogWindow
  (log   [this message] "Log a message to the given log-window")
  (clear [this] "Clear the contents of the log-window"))

(defn logf 
  "Log a formatted message to the given log-window."
  [this fmt & args]
  (log this (apply format fmt args)))

(defn log-window
  "An auto-scrolling log window.
  
  The returned widget implements the LogWindow protocol with 
  which you can clear it, or append messages. It is thread-safe, 
  i.e. messages logged from multiple threads won't be interleaved. 
  
  It must be wrapped in (seesaw.core/scrollable) for scrolling.

  
  Includes a context menu with options for clearing the window
  and scroll lock.

  Returns a sub-class of javax.swing.JTextArea so any of the options
  that apply to multi-line (seesaw.core/text) apply. Also supports
  the following additional options:

    :limit Maximum number of chars to keep in the log. When this limit
           is reached, chars will be removed from the beginning.

  See:
    (seesaw.core/text)
  "
  [& opts]
  (let [scroll-item (checkbox-menu-item :resource ::scroll
                                        :selected? true)
        ; Buffer text from other threads here
        buffer (StringBuffer.)

        limit (atom nil)

        ; Efficiently tell the ui thread to grab the buffer
        ; contents and move it to the text area.
        signal (signaller [^javax.swing.JTextArea ta]
                 (locking buffer
                   (.append ta (str buffer))
                   (.setLength buffer 0))
                 (if-let [limit @limit]
                   (let [doc (config ta :model)
                         length (.getLength doc)]
                     (if (> length limit)
                       (.remove doc 0 (- length limit))))))

        ta (proxy [javax.swing.JTextArea 
                   seesaw.widgets.log_window.LogWindow
                   seesaw.widget_options.WidgetOptionProvider] []
             
             ; Implement LogWindow protocol
             (log [message]
               (.append buffer message)
               (signal this))
             (clear [] (invoke-soon (text! this "")))

             ; Implement WidgetOptionProvider so we can add the :limit
             ; option.
             (get_widget_option_map_STAR_ [] 
               [text-area-options
                (option-map
                  (default-option :limit
                    (fn [_ v] (reset! limit v))
                    (fn [_] @limit)
                    ["An integer limit or nil"]))])
             (get_layout_option_map_STAR_ [] nil)
             
             ; this is how you disable auto-scrolling :(
             (scrollRectToVisible [rect]
               (if (selection scroll-item)
                 (proxy-super scrollRectToVisible rect))))
        
        clear-action (action :resource ::clear
                             :handler (fn [_] (clear ta)))]

    (map-key ta ::key.clear clear-action)

    ; When auto-scroll is re-enabled, have to force caret to bottom
    ; otherwise, it has no effect.
    (listen scroll-item :selection
            (fn [e] (if (selection scroll-item)
                      (scroll! ta :to :bottom))))

    ; Apply default options and whatever options are provided.
    (apply-options
      ta 
      (concat
        [:editable? false
         :font      :monospaced
         :popup     (popup :items [clear-action scroll-item])]
        opts))))

