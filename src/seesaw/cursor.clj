;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions for creating Swing cursors."
      :author "Dave Ray"}
  seesaw.cursor
  (:use [seesaw.util :only [constant-map illegal-argument]])
  (:import [java.awt Cursor Toolkit]))

(def ^{:private true} built-in-cursor-map
  (constant-map Cursor {:suffix "_CURSOR"}
    :crosshair :custom :default :hand :move :text :wait
    :e-resize :n-resize :ne-resize :nw-resize :s-resize :se-resize :sw-resize :w-resize))

(defn- custom-cursor
  [^java.awt.Image image & [point]]
  (let [[x y] point]
    (.. (Toolkit/getDefaultToolkit) (createCustomCursor image
                                                        (java.awt.Point. (or x 0) (or y 0))
                                                        (str (gensym "seesaw-cursor"))))))
(defn cursor
  "Create a built-in or custom cursor. Take one of two forms:

    (cursor :name-of-built-in-cursor)

  Creates a built-in cursor of the given type. Valid types are:

    :crosshair :custom :default :hand :move :text :wait
    :e-resize :n-resize :ne-resize :nw-resize
    :s-resize :se-resize :sw-resize :w-resize

  To create custom cursor:

    (cursor image-or-icon optional-hotspot)

  where image-or-icon is a java.awt.Image (see seesaw.graphics/buffered-image)
  or javax.swing.ImageIcon (see seesaw.icon/icon). The hotspot is an optional
  [x y] point indicating the click point for the cursor. Defaults to [0 0].

  Examples:

    ; The hand cursor
    (cursor :hand)

    ; Create a custom cursor from a URL:
    (cursor (icon \"http://path/to/my/cursor.png\") [5 5])

  Notes:
    This function is used implicitly by the :cursor option on most widget
    constructor functions. So

        (label :cursor (cursor :hand))

    is equivalent to:

        (label :cursor :hand)

    Same for setting the cursor with (seesaw.core/config!).

    Also, the size of a cursor is platform dependent, so some experimentation
    will be required with creating custom cursors from images.

  See:

    http://download.oracle.com/javase/6/docs/api/java/awt/Cursor.html
    http://download.oracle.com/javase/6/docs/api/java/awt/Toolkit.html#createCustomCursor%28java.awt.Image,%20java.awt.Point,%20java.lang.String%29
  "
  ^java.awt.Cursor
  [type & args]
  (cond
    ; TODO protocol if this gets any more nasty
    (keyword? type) (Cursor. (built-in-cursor-map type))
    (instance? Cursor type) type
    (instance? java.awt.Image type) (apply custom-cursor type args)
    (instance? javax.swing.ImageIcon type) (apply cursor (.getImage ^javax.swing.ImageIcon type) args)
    :else (illegal-argument "Don't know how to make cursor from %s" type)))


