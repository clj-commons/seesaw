(defproject seesaw "1.0.5"
  :description "A Swing wrapper/DSL for Clojure. See http://seesaw-clj.org for more info."
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
                 [com.miglayout/miglayout "3.7.4"]]
  :dev-dependencies [[com.stuartsierra/lazytest "1.1.2"]
                     [lein-clojars "0.6.0"]]
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"})

