(defproject seesaw "1.0.4-SNAPSHOT"
  :description "A Swing wrapper/DSL for Clojure"
  :warn-on-reflection true
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [matchure "0.10.1"]
                 [com.miglayout/miglayout "3.7.4"]]
  :dev-dependencies [[com.stuartsierra/lazytest "1.1.2"]
                     [lein-autotest "1.1.0"]
                     [lein-clojars "0.6.0"]]
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"})

