;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.pref)

(defn ^java.util.prefs.Preferences 
  preferences-node*
  "Return the java.util.prefs.Preferences/userRoot for the specified
namespace."
  ([ns]
     (.node (java.util.prefs.Preferences/userRoot) (str (ns-name ns)))))

(defmacro preferences-node
  "Return the java.util.prefs.Preferences/userRoot for the current
  or the specified namespace."
  ([]
     `(preferences-node* ~*ns*))
  ([ns]
     `(preferences-node* ~ns)))

(defn- serialize-value [v]
  (binding [*print-dup* true] (pr-str v)))

(defn bind-preference-to-atom*
  "Bind atom to preference by syncing it
  with (java.util.prefs.Preferences/userRoot) for the specified
  namespace and a given KEY. If no preference has been set yet the
  atom will stay untouched, otherwise it will be set to the stored
  preference value. Note that any value of the atom and the preference
  key must be printable per PRINT-DUP and readable per READ-STRING for
  it to be used with the preferences store."
  [ns key atom]
  (let [key (serialize-value key)
        node (preferences-node ns)
        v   (read-string (.get node key (serialize-value @atom)))]
    (doto atom
      (reset! v)
      (add-watch (keyword (gensym "pref-atom-watcher"))
                 (fn [k r o n]
                   (when (not= o n) 
                     (.put node key (serialize-value n))))))))

(defmacro bind-preference-to-atom
  "Bind atom to preference by syncing it
  with (java.util.prefs.Preferences/userRoot) for the current
  namespace and a given KEY. If no preference has been set yet the
  atom will stay untouched, otherwise it will be set to the stored
  preference value. Note that any value of the atom and the preference
  key must be printable per PRINT-DUP and readable per READ-STRING for
  it to be used with the preferences store."
  [key atom]
  `(bind-preference-to-atom* ~*ns* ~key ~atom))

(defmacro preference-atom
  "Create and return an atom which has been bound using
  bind-preference-to-atom for the current namespace."
  ([key]
     `(let [atom# (atom nil)]
        (bind-preference-to-atom ~key atom#)))
  ([key initial-value]
     `(let [atom# (atom ~initial-value)]
        (bind-preference-to-atom ~key atom#))))

