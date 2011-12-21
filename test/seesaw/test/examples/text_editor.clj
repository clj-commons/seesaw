(ns seesaw.test.examples.text-editor
  (:use seesaw.core
        seesaw.chooser
        seesaw.mig
        seesaw.test.examples.example
        [clojure.java.io :only [file]]))

(native!)

(def current-file (atom (file (System/getProperty "user.home") ".sescratch")))

(when-not (.exists @current-file) (spit @current-file ""))

(def current-file-label (label :text @current-file :font "SANSSERIF-PLAIN-8"))

(def editor (editor-pane :text (slurp @current-file)))

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

(defn select-file [type] (choose-file main-panel :type type))

(defn a-new [e]
  (let [selected (select-file :save)] 
    (if (.exists @current-file)
      (alert "File already exists.")
      (do (set-current-file selected)
          (text! editor "")
          (set-status "Created a new file.")))))

(defn a-open [e]
  (let [selected (select-file :open)] (set-current-file selected))
  (text! editor (slurp @current-file))
  (set-status "Opened " @current-file "."))

(defn a-save [e]
  (spit @current-file (text editor))
  (set-status "Wrote " @current-file "."))

(defn a-save-as [e]
  (when-let [selected (select-file :save)]
    (set-current-file selected)
    (spit @current-file (text editor))
    (set-status "Wrote " @current-file ".")))

(defn a-exit  [e] (dispose! e))
(defn a-copy  [e] (.copy editor))
(defn a-cut   [e] (.cut editor))
(defn a-paste [e] (.paste editor))

(def menus
     (let [a-new (action :handler a-new :name "New" :tip "Create a new file." :key "menu N")
           a-open (action :handler a-open :name "Open" :tip "Open a file" :key "menu O")
           a-save (action :handler a-save :name "Save" :tip "Save the current file." :key "menu S")
           a-exit (action :handler a-exit :name "Exit" :tip "Exit the editor.")
           a-copy (action :handler a-copy :name "Copy" :tip "Copy selected text to the clipboard." :key "menu C")
           a-paste (action :handler a-paste :name "Paste" :tip "Paste text from the clipboard." :key "menu V")
           a-cut (action :handler a-cut :name "Cut" :tip "Cut text to the clipboard." :key "menu X")
           a-save-as (action :handler a-save-as :name "Save As" :tip "Save the current file." :key "menu shift S")]
       (menubar
        :items [(menu :text "File" :items [a-new a-open a-save a-save-as a-exit])
                (menu :text "Edit" :items [a-copy a-cut a-paste])])))

(defexample []
  (add-watch
    current-file
    nil
    (fn [_ _ _ new] (text! current-file-label (str new))))
  (frame
    :title "Seesaw Example Text Editor"
    :content main-panel
    :minimum-size [640 :by 480]
    :menubar menus))

;(run :dispose)

