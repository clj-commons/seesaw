(defproject seesaw "1.0.2-SNAPSHOT"
  :description "A Swing wrapper/DSL for Clojure"
  :warn-on-reflection true
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [com.miglayout/miglayout "3.7.4"]
                 [net.java.dev.designgridlayout/designgridlayout "1.7"]]

  :dev-dependencies [[com.stuartsierra/lazytest "1.1.2"]
                     [lein-autotest "1.1.0"]
                     [lein-clojars "0.6.0"]]
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"})

