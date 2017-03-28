;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "File chooser and other common dialogs."
      :author "Dave Ray"}
  seesaw.chooser
  (:use [seesaw.color :only [to-color]]
        [seesaw.options :only [default-option bean-option apply-options 
                               option-map option-provider]]
        [seesaw.util :only [illegal-argument]])
  (:import (javax.swing.filechooser FileFilter FileNameExtensionFilter)
           [javax.swing JFileChooser]))

(defn file-filter
  "Create a FileFilter.
  
  Arguments:
  
    description - description of this filter, will show up in the
                  filter-selection box when opening a file choosing dialog.

    accept - a function taking a java.awt.File
             returning true if the file should be shown,
             false otherwise.
  "
  [description accept]
  (proxy [FileFilter] []
    (accept [file]
      (if (accept file)
        true false))
    (getDescription []
      description)))

(def ^{:private true} file-chooser-types {
  :open   JFileChooser/OPEN_DIALOG
  :save   JFileChooser/SAVE_DIALOG
  :custom JFileChooser/CUSTOM_DIALOG
})

(def ^{:private true} file-selection-modes {
  :files-only     JFileChooser/FILES_ONLY
  :dirs-only      JFileChooser/DIRECTORIES_ONLY
  :files-and-dirs JFileChooser/FILES_AND_DIRECTORIES
})

(defn set-file-filters [^JFileChooser chooser filters]
  (.resetChoosableFileFilters chooser)
  (doseq [f filters]
    (.addChoosableFileFilter chooser
      (cond
        (instance? FileFilter f) 
          f

        (and (sequential? f) (sequential? (second f)))
          (FileNameExtensionFilter. (first f) (into-array (second f)))

        (and (sequential? f) (fn? (second f)))
          (apply file-filter f)

        :else
        (illegal-argument "not a valid filter: %s" f)))))

(defn- set-suggested-name [^JFileChooser chooser suggested-name]
  (.setSelectedFile chooser (if (instance? java.io.File suggested-name)
                              suggested-name (java.io.File. suggested-name))))

(def ^{:private true} file-chooser-options 
  (option-map
    (default-option :dir
      (fn [^JFileChooser chooser dir] 
        (.setCurrentDirectory chooser (if (instance? java.io.File dir) dir 
                                          (java.io.File. (str dir))))))
    (bean-option [:multi? :multi-selection-enabled] JFileChooser boolean)
    (bean-option [:selection-mode :file-selection-mode] JFileChooser file-selection-modes)
    (default-option :filters set-file-filters)
    (bean-option [:all-files? :accept-all-file-filter-used] JFileChooser boolean)
    (default-option :suggested-name set-suggested-name)))

(option-provider JFileChooser file-chooser-options)

(def ^{:private true} last-dir (atom nil))

(defn- show-file-chooser [^JFileChooser chooser parent type]
  (case type
    :open (.showOpenDialog chooser parent) 
    :save (.showSaveDialog chooser parent)
          (.showDialog chooser parent (str type))))

(defn- configure-file-chooser [^JFileChooser chooser opts]
  (apply-options chooser opts)
  (when (and @last-dir (not (:dir opts)))
    (.setCurrentDirectory chooser @last-dir))
  chooser)

(defn- remember-chooser-dir [^JFileChooser chooser]
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
    :selection-mode The file selection mode: :files-only, :dirs-only and :files-and-dirs.
                    Defaults to :files-only
    :filters A seq of either:

               a seq that contains a filter name and a seq of
               extensions as strings for that filter;

               a seq that contains a filter name and a function
               to be used as accept function (see file-filter);

               a FileFilter (see file-filter).

             The filters appear in the dialog's filter selection in the same
             order as in the seq.
    :all-files? If true, a filter matching all file extensions and files
                without an extension will appear in the filter selection
                of the dialog additionally to the filters specified
                through :filters. The filter usually appears last in the
                selection. If this is not desired set this option to
                false and include an equivalent filter manually at the
                desired position as shown in the examples below. Defaults
                to true.

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

    ; ask & return including a filter for image files and an \"all files\"
    ; filter appearing at the beginning
    (choose-file :all-files? false
                 :filters [(file-filter \"All files\" (constantly true))
                           [\"Images\" [\"png\" \"jpeg\"]]
                           [\"Folders\" #(.isDirectory %)]])

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
        ^JFileChooser chooser (configure-file-chooser
                                (JFileChooser.)
                                (dissoc
                                  opts
                                  :type
                                  :remember-directory?
                                  :success-fn
                                  :cancel-fn))]
    (when-let [[filter _] (seq (.getChoosableFileFilters chooser))]
      (.setFileFilter chooser filter))
    (let [result (show-file-chooser chooser parent type)
          multi? (.isMultiSelectionEnabled chooser)]
      (cond
        (= result JFileChooser/APPROVE_OPTION)
          (do
            (when remember-directory?
              (remember-chooser-dir chooser))
            (success-fn
              chooser
              (if multi?
                (.getSelectedFiles chooser)
                (.getSelectedFile chooser))))
        :else (cancel-fn chooser)))))

(defn choose-color
  "Choose a color with a color chooser dialog. The optional first argument is the
  parent component for the dialog. The rest of the args is a list of key/value 
  pairs:
  
          :color The initial selected color (see seesaw.color/to-color)
          :title The dialog's title
  
  Returns the selected color or nil if canceled.
  
  See:
    http://download.oracle.com/javase/6/docs/api/javax/swing/JColorChooser.html
  "
  [& args]
  (let [[parent & {:keys [color title]
                   :or { title "Choose a color"}
                   :as opts}] (if (keyword? (first args)) (cons nil args) args)
        parent (if (keyword? parent) nil parent)]
    (javax.swing.JColorChooser/showDialog parent title (to-color color))))
