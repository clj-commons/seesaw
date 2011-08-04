(defproject seesaw "1.1.0"
  :description "A Swing wrapper/DSL for Clojure. You want seesaw.core, FYI. See http://seesaw-clj.org for more info."
  :url "http://seesaw-clj.org"
  :mailing-list {:name "seesaw-clj"
                 :achive "https://groups.google.com/group/seesaw-clj"
                 :post "seesaw-clj@googlegroups.com"}
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :warn-on-reflection true
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [com.miglayout/miglayout "3.7.4"]
                 [com.jgoodies/forms "1.2.1"]]
  :dev-dependencies [[com.stuartsierra/lazytest "1.1.2"]
                     [lein-clojars "0.6.0"]
                     [org.clojars.rayne/autodoc "0.8.0-SNAPSHOT"]]
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"}
  :autodoc {
    :name "Seesaw", 
    :page-title "Seesaw API Documentation"
    :copyright "Copyright 2011, Dave Ray"
    :load-except-list [#"seesaw/examples"]})

