;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.pref)

(defmacro preferences-node
  "Return the java.util.prefs.Preferences/userRoot for the current
  namespace."
  []
  `(.node (java.util.prefs.Preferences/userRoot) ~(str (ns-name *ns*))))

(defn bind-preference-to-atom
  "Generate and return an atom, which will automatically be synced
  with (java.util.prefs.Preferences/userRoot) for the current
  namespace and a given string KEY. If not yet set, the atom will have
  INITIAL-VALUE as its value, or the value which has already been set
  inside the preferences. Note that the value must be printable per
  PRINT-DUP and readable per READ-STRING for it to be used with the
  preferences store."  
  [key initial-value]
  ; TODO This doesn't work because preferences-node will store in the seesaw.pref
  ; namespace, not the namespace of the caller!
  (let [v (atom (read-string (.get (preferences-node) key (binding [*print-dup* true] (pr-str initial-value)))))]
    (add-watch v (keyword (gensym "pref-atom-watcher"))
               (fn [k r o n] (when (not= o n) 
                               (.put (preferences-node) key (binding [*print-dup* true] (pr-str n))))))))

