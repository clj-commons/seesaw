_Note that current development is on the *develop* branch, not master_

There's now a [Google Group] (https://groups.google.com/group/seesaw-clj) for discussion and questions.

# Seesaw: Clojure + UI

_"killing CamelCase, proxy and reify, one class at a time"_

*_Seesaw's experimental and subject to radical change_*

_*See [the Seesaw Wiki] (https://github.com/daveray/seesaw/wiki) and [the Seesaw API Docs] (http://daveray.github.com/seesaw/) for more detailed docs. Note that the docs in the code (use the `doc` function!) are always the most up-to-date and trustworthy.*_

Seesaw is a library/DSL for constructing user interfaces in Clojure. It happens to be built on Swing, but please don't hold that against it. It's an experiment to see what I can do to make user interface development funner in Clojure. It's kinda inspired by [Shoes](http://shoesrb.com/), [Stuart Sierra's Swing posts](http://stuartsierra.com/tag/swing), etc. [clojure.contrib.swing-utils](http://richhickey.github.com/clojure-contrib/swing-utils-api.html) is useful, but minimal and still means a lot of "Java-in-Clojure" coding.


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
                    [seesaw "x.y.z"]])

_Replace the Seesaw version with whatever the latest version tag is. See below!_

Now edit the generated `src/hello_seesaw/core.clj` file:

    (ns hello-seesaw.core
      (:use seesaw.core))

    (defn -main [& args]
      (invoke-later 
        (-> (frame :title "Hello", 
               :content "Hello, Seesaw",
               :on-close :exit)
         pack!
         show!)))

Now run it:

    $ lein run -m hello-seesaw.core

*NOTE:* As mentioned above, Seesaw is experimental and changing daily. Thus, there's a good chance that what's on clojars and what's written here are out of sync. Here's how you can run against the bleeding edge of Seesaw:

* Clone Seesaw from github. Fork if you like. *Switch to the "develop" branch.*
* In your Seesaw checkout, run `lein install` to build it
* In your project's `project.clj` file, change the Seesaw version to `X.Y.Z-SNAPSHOT` to match whatever's in Seesaw's `project.clj`.
* Run `lein deps`
* Move along

## License

Copyright (C) 2011 Dave Ray

Distributed under the Eclipse Public License, the same as Clojure.
