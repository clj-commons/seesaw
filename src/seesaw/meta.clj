;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for associating metadata with frames and widgets, etc."
      :author "Dave Ray"}
  seesaw.meta)

(defprotocol Meta
  (put-meta! [this key value])
  (get-meta  [this key]))

(def ^{:private true} meta-map (java.util.WeakHashMap.))

(extend-protocol Meta
  Object
    (put-meta! [this key value]
      (if-let [this-map (.get ^java.util.Map meta-map this)]
        (.put ^java.util.Map this-map key value)
        (.put ^java.util.Map meta-map this (doto (java.util.HashMap.)
                                             (.put key value))))
      this)
    (get-meta  [this key]
      (when-let [this-map (.get ^java.util.Map meta-map this)]
        (.get ^java.util.Map this-map key)))

  javax.swing.JComponent
    (put-meta! [this key value] (doto this (.putClientProperty key value)))
    (get-meta  [this key] (.getClientProperty this key))
  javax.swing.Action
    (put-meta! [this key value] (doto this (.putValue (str key) value)))
    (get-meta  [this key] (.getValue this (str key))))


