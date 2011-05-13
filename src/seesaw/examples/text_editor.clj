(ns seesaw.examples.text-editor
  (:use seesaw.core
        [clojure.java.io :only [file]])
  (:import [javax.swing JFileChooser JEditorPane]))

(def current-file (atom (file (System/getProperty "user.home") ".sescratch")))

(when-not (.exists @current-file) (spit @current-file ""))

(def current-file-label (label :text @current-file :font "SANSSERIF-PLAIN-8"))

(def editor (doto (JEditorPane.) (.setText (slurp @current-file))))

(def status-label (label :text "Your text. It goes there."))

(defn set-status [& strings] (text! status-label (apply str strings)))

(def main-panel
     (mig-panel
      :constraints ["fill, ins 0"]
      :items [[(scrollable editor) "grow"]
              [status-label "dock south"]
              [(separator) "dock south"]
              [current-file-label "dock south"]]))

(defn set-current-file [f] (swap! current-file (constantly f)))

(defn select-file []
  (let [chooser (JFileChooser.)]
    (.showDialog chooser main-panel "Select")
    (.getSelectedFile chooser)))

(defn a-new [e]
  (let [selected (select-file)] 
    (if (.exists @current-file)
      (alert "File already exists.")
      (do (set-current-file selected)
          (.setText editor "")
          (set-status "Created a new file.")))))

(defn a-open [e]
  (let [selected (select-file)] (set-current-file selected))
  (.setText editor (slurp @current-file))
  (set-status "Opened " @current-file "."))

(defn a-save [e]
  (spit @current-file (.getText editor))
  (set-status "Wrote " @current-file "."))

(defn a-save-as [e]
  (when-let [selected (select-file)]
    (set-current-file selected)
    (spit @current-file)
    (set-status "Wrote " @current-file ".")))

(defn a-exit  [e] (System/exit 0))
(defn a-copy  [e] (.copy editor))
(defn a-cut   [e] (.cut editor))
(defn a-paste [e] (.paste editor))

(def menus
     (let [a-new (action :handler a-new :name "New" :tip "Create a new file.")
           a-open (action :handler a-open :name "Open" :tip "Open a file")
           a-save (action :handler a-save :name "Save" :tip "Save the current file.")
           a-exit (action :handler a-exit :name "Exit" :tip "Exit the editor.")
           a-copy (action :handler a-copy :name "Copy" :tip "Copy selected text to the clipboard.")
           a-paste (action :handler a-paste :name "Paste" :tip "Paste text from the clipboard.")
           a-cut (action :handler a-cut :name "Cut" :tip "Cut text to the clipboard.")
           a-save-as (action :handler a-save-as :name "Save As" :tip "Save the current file.")]
       (menubar
        :items [(menu :text "File" :items [a-new a-open a-save a-save-as a-exit])
                (menu :text "Edit" :items [a-copy a-cut a-paste])])))

(defn -main [& args]
  (add-watch
   current-file
   nil
   (fn [_ _ _ new] (text! current-file-label (str new))))
  (invoke-now
   (frame
    :title "Seesaw Example Text Editor"
    :content main-panel
    :on-close :exit
    :minimum-size [640 :by 480]
    :menubar menus)))
