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
        [seesaw.bind :only [bind]]
        [seesaw.keymap :only [map-key]]
        [seesaw.invoke :only [signaller]]
        [seesaw.options :only [apply-options option-map default-option]]
        [seesaw.widget-options :only [WidgetOptionProvider]]))

(defn- log-window-proxy [state]
  (proxy [javax.swing.JTextArea clojure.lang.IDeref] []
    ; Implement IDeref
    (deref [] state) 

    ; this is how you disable auto-scrolling :(
    (scrollRectToVisible [rect]
                         (if @(:auto-scroll? state)
                           (proxy-super scrollRectToVisible rect)))))

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

    :auto-scroll? Whether the window should auto-scroll. This is the
          programmatic hook for the context menu entry.

  See:
    (seesaw.core/text)
  "
  [& opts]
  (let [state {:buffer (StringBuffer.) ; Buffer text from other threads here
               :limit  (atom nil) 
               :auto-scroll? (atom true)
                ; Efficiently tell the ui thread to grab the buffer
                ; contents and move it to the text area.
               :signal (signaller [this]
                         (let [{:keys [buffer limit]} @this] 
                           (locking buffer
                             (.append this (str buffer))
                             (.setLength buffer 0))
                           (if-let [limit @limit]
                             (let [doc    (config this :model)
                                   length (.getLength doc)]
                               (if (> length limit)
                                 (.remove doc 0 (- length limit))))))) }

        this (log-window-proxy state) 

        scroll-item (checkbox-menu-item :resource ::scroll
                                        :selected? true)
        
        clear-action (action :resource ::clear
                             :handler (fn [_] (clear this)))]

    (map-key this ::key.clear clear-action)

    (bind scroll-item (:auto-scroll? state) scroll-item)

    ; When auto-scroll is re-enabled, have to force caret to bottom
    ; otherwise, it has no effect.
    (listen scroll-item :selection
            (fn [e] (if (selection scroll-item)
                      (scroll! this :to :bottom))))

    ; Apply default options and whatever options are provided.
    (apply-options
      this 
      (concat
        [:editable? false
         :font      :monospaced
         :popup     (popup :items [clear-action scroll-item])]
        opts))))

(extend-type (class (log-window-proxy nil))

  LogWindow
  (log [this message]
    (let [{:keys [buffer signal]} @this]
      (.append buffer message)
      (signal this)))
  (clear [this] 
    (invoke-soon (text! this "")))

  WidgetOptionProvider 
  (get-widget-option-map* [this] 
    [text-area-options
     (option-map
       (default-option :limit
         (fn [this v] (reset! (:limit @this) v))
         (fn [this] @(:limit @this))
         ["An integer limit or nil"])
       (default-option :auto-scroll?
         (fn [this v] (reset! (:auto-scroll? @this) v))
         (fn [this]   @(:auto-scroll? @this))))])
  (get-layout-option-map* [this] nil))

