(defproject seesaw "1.4.2-SNAPSHOT"
  :description "A Swing wrapper/DSL for Clojure. You want seesaw.core, FYI. See http://seesaw-clj.org for more info."

  :url "http://seesaw-clj.org"

  :mailing-list {:name "seesaw-clj"
                 :archive "https://groups.google.com/group/seesaw-clj"
                 :post "seesaw-clj@googlegroups.com"}

  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}

  :warn-on-reflection true

  ; To run the examples:
  ;
  ;   $ lein run :examples
  ;
  :run-aliases { :examples seesaw.test.examples.launcher }

  :dependencies [[org.clojure/clojure "1.3.0"]
                 [com.miglayout/miglayout "3.7.4"]
                 [com.jgoodies/forms "1.2.1"]
                 [org.swinglabs.swingx/swingx-core "1.6.3"]
                 [j18n "1.0.1"]
                 [org.fife.ui/rsyntaxtextarea "2.0.3"]]
  :dev-dependencies [[com.stuartsierra/lazytest "1.1.2"]
                     [lein-clojars "0.7.0"]
                     [lein-autodoc "0.9.0"]]
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"}
  :autodoc {
    :name "Seesaw",
    :page-title "Seesaw API Documentation"
    :copyright "Copyright 2012, Dave Ray" }
  :java-source-path "jvm")

