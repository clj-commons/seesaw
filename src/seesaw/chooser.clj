;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.chooser
  (:use seesaw.util)
  (:import [javax.swing JFileChooser]))

(def ^{:private true} file-chooser-types {
  :open   JFileChooser/OPEN_DIALOG
  :save   JFileChooser/SAVE_DIALOG
  :custom JFileChooser/CUSTOM_DIALOG
})

(def ^{:private true} file-chooser-options {
  :dir    #(.setCurrentDirectory %1 %2)
  :multi? #(.setMultiSelectionEnabled %1 (boolean %2))
  :filters #(doseq [[name exts] %2]
              (.setFileFilter %1 (javax.swing.filechooser.FileNameExtensionFilter. name (into-array exts))))
})

(def ^{:private true} last-dir (atom nil))

(defn- show-file-chooser [chooser parent type]
  (case type
    :open (.showOpenDialog chooser parent) 
    :save (.showSaveDialog chooser parent)
          (.showDialog chooser parent (str type))))

(defn- configure-file-chooser [chooser opts]
  (apply-options chooser opts file-chooser-options)
  (when (and @last-dir (not (:dir opts)))
    (.setCurrentDirectory chooser @last-dir))
  chooser)

(defn- remember-chooser-dir [chooser]
  (reset! last-dir (.getCurrentDirectory chooser))
  chooser)

(defn choose-file
  "Choose a file to open or save. The arguments can take two forms. First, with
  an initial parent component which will act as the parent of the dialog.

      (choose-file dialog-parent ... options ...)

  If the first arg is omitted, the desktop is used as the parent of the dialog:

      (choose-file ... options ...)

  Options can be one of:

    :type The dialog type: :open, :save, or a custom string placed on the Ok button.
          Defaults to :open.
    :dir  The initial working directory. If omitted, the previous directory chosen
          is remembered and used.
    :multi?  If true, multi-selection is enabled and a seq of files is returned.
    :filters A seq of lists where each list contains a filter name and a seq of
             extensions as strings for that filter. Default: [].
    :remember-directory? Flag specifying whether to remember the directory for future
                         file-input invocations in case of successful exit. Default: true.
    :success-fn  Function which will be called with the JFileChooser and the File which
                 has been selected by the user. Its result will be returned.
                 Default: return selected File. In the case of MULTI-SELECT? being true,
                 a seq of File instances will be passed instead of a single File.
    :cancel-fn   Function which will be called with the JFileChooser on user abort of the dialog.
                 Its result will be returned. Default: returns nil.

  Examples:

    ; ask & return single file
    (choose-file)

    ; ask & return including a filter for image files
    (choose-file :filters [[\"Images\" [\"png\" \"jpeg\"]]])

    ; ask & return absolute file path as string
    (choose-file :success-fn (fn [fc file] (.getAbsolutePath file)))

  Returns result of SUCCESS-FN (default: either java.io.File or seq of java.io.File iff multi? set to true)
  in case of the user selecting a file, or result of CANCEL-FN otherwise.
  
  See http://download.oracle.com/javase/6/docs/api/javax/swing/JFileChooser.html
  "
  [& args]
  (let [[parent & {:keys [type remember-directory? success-fn cancel-fn]
                   :or {type :open
                        remember-directory? true
                        success-fn (fn [fc files] files)
                        cancel-fn (fn [fc])}
                   :as opts}] (if (keyword? (first args)) (cons nil args) args)
        parent  (if (keyword? parent) nil parent)
        chooser (configure-file-chooser (JFileChooser.) (dissoc opts :type :remember-directory? :success-fn :cancel-fn))
        multi?  (.isMultiSelectionEnabled chooser)
        result  (show-file-chooser chooser parent type)]
    (cond 
      (= result JFileChooser/APPROVE_OPTION)
        (do
          (when remember-directory?
            (remember-chooser-dir chooser))
          (success-fn chooser (if multi? (.getSelectedFiles chooser) (.getSelectedFile chooser))))
      :else (cancel-fn chooser))))

