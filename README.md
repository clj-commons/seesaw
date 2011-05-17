# Seesaw: a Clojure/Swing experiment

_Seesaw's experimental and subject to radical change_

Seesaw's an experiment to see what I can do to make Swing funner in Clojure. It's kinda inspired by [Shoes](http://shoesrb.com/), [Stuart Sierra's Swing posts](http://stuartsierra.com/tag/swing), etc. [clojure.contrib.swing-utils](http://richhickey.github.com/clojure-contrib/swing-utils-api.html) is useful, but minimal and still means a lot of "Java-in-Clojure" coding.

*See [the Seesaw Wiki] (https://github.com/daveray/seesaw/wiki) for more detailed docs*

## TL;DR

Here's how you use Seesaw with [Leiningen] (https://github.com/technomancy/leiningen)

Install `lein` as described and then:

    $ lein new hello-seesaw
    $ cd hello-seesaw

Add Seesaw to `project.clj`

    (defproject hello-seesaw "1.0.0-SNAPSHOT"
      :description "FIXME: write"
      :dependencies [[org.clojure/clojure "1.2.0"]
                    [org.clojure/clojure-contrib "1.2.0"]
                    [seesaw "1.0.1"]])

_Replace the Seesaw version with whatever the latest version tag is in case I forget to update this._

Now edit the generated `src/hello_seesaw/core.clj` file:

    (ns hello-seesaw.core
      (:use seesaw.core))

    (defn -main [& args]
      (invoke-later 
        (frame :title "Hello", 
               :content "Hello, Seesaw",
               :on-close :exit)))

Now run it:

    $ lein run -m hello-seesaw.core


## TODO

* A non-trivial example app to see if this stuff holds up
* GridBagLayout needs more work
* Graphics - I'd rather not wrap the entire Java2D API if there's already something out there (maybe processing?) that does that.
* Structural manip - add/remove widgets to containers.
* Selectors - select widgets by class, data, etc.
* Styling
* Some kind of ToModel protocol for auto-converting Clojure data-structures to Swing models.
* Investigate how a framework like [cljque] (https://github.com/stuartsierra/cljque) might fit in with Seesaw



## License

Copyright (C) 2011 Dave Ray

Distributed under the Eclipse Public License, the same as Clojure.
