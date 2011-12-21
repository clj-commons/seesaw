(ns seesaw.test.examples.form
  (:use seesaw.core
        seesaw.test.examples.example))

;http://www.leepoint.net/notes-java/GUI/layouts/gridbag-example.html 

(defexample []
  (frame :title "Find/Replace" :content 
    (form-panel
      :items [
        [nil :fill :both :insets (java.awt.Insets. 5 5 5 5) :gridx 0 :gridy 0]

        [(label :text "Find What:" :halign :right)]

        [(text :columns 20) :grid :next :weightx 1.0]

        [(grid-panel :columns 1
              :items (map #(button :text %) ["Find" "Replace" "Replace All" "Close" "Help"]))
          :grid :next :gridheight 5 :anchor :north :weightx 0]

        [(label :text "Replace With:" :halign :right) :gridheight 1 :grid :wrap]

        [(text :columns 20) :grid :next :weightx 1.0]

        [[5 :by 5] :grid :wrap]

        [(grid-panel :columns 2 
            :items (map #(checkbox :text %) 
                      ["Match Case" "Wrap Around" 
                       "Whole Words" "Search Solution"
                       "Regular Expressions" "Search Backwards"
                       "Highlight Results" "Incremental Search"]))
          :grid :next]]
        )))

;(run :dispose)

