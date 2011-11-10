;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for configuring widgets. Prefer (seesaw.core/config) and friends."
      :author "Dave Ray"}
  seesaw.config
  (:use [seesaw.util :only [to-seq]]))

(defprotocol Configurable
  "A protocol for configuring and querying properties of an object. Client
  code should use (seesaw.core/config!) and (seesaw.core/config) rather than
  calling protocol methods directly.
  
  See:
    (seesaw.core/config)
    (seesaw.core/config!)
  "
  (config!* [target args] "Configure one or more options on target. Args is a list of key/value pairs. See (seesaw.core/config!)")
  (config* [target name] "Retrieve the current value for the given named option. See (seesaw.core/config)"))

(defn config
  "Retrieve the value of an option from target. For example:
  
    (config button1 :text)
    => \"I'm a button!\"
  
  Target must satisfy the Configurable protocol. In general, it may be a widget, 
  or convertible to widget with (to-widget). For example, the target can be an event 
  object.

  Returns the option value. 
  Throws IllegalArgumentException if an unknown option is requested.

  See:
    (seesaw.core/config!)
  "
  [target name]
  (config* target name))

(defn config!
  "Applies options in the argument list to one or more targets. For example:

    (config! button1 :enabled? false :text \"I' disabled\")

  or:

    (config! [button1 button2] :enabled? false :text \"We're disabled\")
 
  Targets must satisfy the Configurable protocol. In general, they may be widgets, 
  or convertible to widgets with (to-widget). For example, the target can be an event 
  object.

  Returns the input targets.
  Throws IllegalArgumentException if an unknown option is encountered.

  See:
    (seesaw.core/config)
  "
  [targets & args]
  (doseq [target (to-seq targets)]
    (config!* target args))
  targets)

