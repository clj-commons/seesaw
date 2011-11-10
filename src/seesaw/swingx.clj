;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "SwingX integration. Unfortunately, SwingX is hosted on java.net which means
           it looks abandoned most of the time. Downloads are here 
           http://java.net/downloads/swingx/releases/1.6/"
      :author "Dave Ray"}
  seesaw.swingx
  (:use [seesaw.util :only [to-uri resource]]
        [seesaw.icon :only [icon]]
        [seesaw.core :only [construct default-options button-options label-options ConfigIcon get-icon set-icon]]
        [seesaw.options :only [option-map bean-option apply-options default-option resource-option]]))

(def xlabel-options
  (merge
    label-options
    (option-map
      ; TODO xlabel text-alignment, painter, etc
      (bean-option [:wrap-lines? :line-wrap?] org.jdesktop.swingx.JXLabel boolean)
      (bean-option :text-rotation org.jdesktop.swingx.JXLabel))))

;*******************************************************************************
; XLabel 

(defn xlabel
  "Creates a org.jdesktop.swingx.JXLabel which is an improved (label) that 
  supports wrapped text, rotation, etc. Additional options:
  
    :wrap-lines? When true, text is wrapped to fit
    :text-rotation Rotation of text in radians

  Examples:

    (xlabel :text        \"This is really a very very very very very very long label\"
            :wrap-lines? true
            :rotation    (Math/toRadians 90.0))

  See:
    (seesaw.core/label)
    (seesaw.core/label-options)
    (seesaw.swingx/xlabel-options)
  "
  [& args]
  (apply-options (construct org.jdesktop.swingx.JXLabel args) args xlabel-options))

;*******************************************************************************
; BusyLabel 

(def busy-label-options
  (merge
    label-options
    (option-map
      ; TODO busy-label text-alignment, painter, etc
      (bean-option :busy? org.jdesktop.swingx.JXBusyLabel boolean))))

(defn busy-label
  "Creates a org.jdesktop.swingx.JXBusyLabel which is a label that shows
  'busy' status with a spinner, kind of like an indeterminate progress bar. 
  Additional options:
  
    :busy? Whether busy status should be shown or not. Defaults to false.

  Examples:

    (busy-label :text \"Processing ...\"
                :busy? true)

  See:
    (seesaw.core/label)
    (seesaw.core/label-options)
    (seesaw.swingx/busy-label-options)
  "
  [& args]
  (apply-options (construct org.jdesktop.swingx.JXBusyLabel args) args busy-label-options))

;*******************************************************************************
; Hyperlink 
(def hyperlink-options
  (merge
    button-options
    (option-map
      (bean-option [:uri :URI] org.jdesktop.swingx.JXHyperlink to-uri))))

(defn hyperlink 
  "Constuct an org.jdesktop.swingx.JXHyperlink which is a button that looks like
  a link and opens its URI in the system browser. In addition to all the options of
  a button, supports:
  
    :uri A string, java.net.URL, or java.net.URI with the URI to open
 
  Examples:

    (hyperlink :text \"Click Me\" :uri \"http://google.com\")

  See:
    (seesaw.core/button)
    (seesaw.core/button-options)
  "
  [& args]
  (apply-options (construct org.jdesktop.swingx.JXHyperlink args) args hyperlink-options))

;*******************************************************************************
; TaskPane

(extend-protocol ConfigIcon
  org.jdesktop.swingx.JXTaskPane 
    (get-icon [this] (.getIcon this))
    (set-icon [this v] 
      (.setIcon this (icon v))))

(def task-pane-options
  (merge
    default-options
    (option-map
      ; TODO I have to add this manually because relying on the impl from default-options
      ; fails with "No implementation of method: :set-icon :(
      (default-option :icon set-icon get-icon)
      (resource-option :resource [:title :icon])
      (bean-option :title org.jdesktop.swingx.JXTaskPane resource)
      (bean-option :animated? org.jdesktop.swingx.JXTaskPane boolean)
      (bean-option :collapsed? org.jdesktop.swingx.JXTaskPane boolean)
      (bean-option :scroll-on-expand? org.jdesktop.swingx.JXTaskPane boolean)
      (bean-option :special? org.jdesktop.swingx.JXTaskPane boolean)
      (default-option :actions
        (fn [^org.jdesktop.swingx.JXTaskPane c actions]
          (doseq [^javax.swing.Action a actions]
            (.add c a)))))))

(defn task-pane
  "Create a org.jdesktop.swingx.JXTaskPane which is a collapsable component with a title
  and icon. It is generally used as an item inside a task-pane-container.  Supports the
  following additional options 
 
    :resource Get icon and title from a resource
    :icon The icon
    :title The pane's title
    :animated? True if collapse is animated
    :collapsed? True if the pane should be collapsed
    :scroll-on-expand? If true, when expanded, it's container will scroll the pane into 
                       view
    :special? If true, the pane will be displayed in a 'special' way depending on 
              look and feel

  The pane can be populated with the standard :items option, which just takes a 
  sequence of widgets. Additionally, the :actions option takes a sequence of
  action objects and makes hyper-links out of them.  

  See:
    (seesaw.swingx/task-pane-options)
    (seesaw.swingx/task-pane-container)
  "
  [& args]
  (apply-options 
    (construct org.jdesktop.swingx.JXTaskPane args) 
    args 
    task-pane-options))

(def task-pane-container-options
  (merge
    default-options
    (option-map
      (default-option 
        :items
        #(doseq [^org.jdesktop.swingx.JXTaskPane p %2] 
           (.add ^org.jdesktop.swingx.JXTaskPaneContainer %1 p))))))

(defn task-pane-container
  "Creates a container for task panes. Supports the following additional
  options: 
  
    :items Sequence of task-panes to display
 
  Examples:

    (task-pane-container 
      :items [(task-pane :title \"First\" 
                :actions [(action :name \"HI\") 
                          (action :name \"BYE\")])
              (task-pane :title \"Second\" 
                :actions [(action :name \"HI\") 
                          (action :name \"BYE\")])
              (task-pane :title \"Third\" :special? true :collapsed? true
                :items [(button :text \"YEP\")])])
  See:
    (seesaw.swingx/task-pane-container-options)
    (seesaw.swingx/task-pane)
  "
  [& args]
  (apply-options 
    (construct org.jdesktop.swingx.JXTaskPaneContainer args) 
    args 
    task-pane-container-options))

