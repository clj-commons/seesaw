[![Build Status](https://secure.travis-ci.org/daveray/seesaw.png?branch=master)](http://travis-ci.org/daveray/seesaw)

There's now a [Google Group](https://groups.google.com/group/seesaw-clj) for discussion and questions.

[Here's a brief tutorial](https://gist.github.com/1441520) that covers some Seesaw basics. It assumes no knowledge of Swing or Java.

[Here's the slides](http://darevay.com/talks/clojurewest2012/) from a Clojure/West 2012 talk on the Seesaw. Best viewed in Chrome or Safari.

# Seesaw: Clojure + UI

_*See [the Seesaw Wiki](https://github.com/daveray/seesaw/wiki) and [the Seesaw API Docs](http://daveray.github.com/seesaw/) for more detailed docs. Note that the docs in the code (use the `doc` function!) are always the most up-to-date and trustworthy.*_

Seesaw is a library/DSL for constructing user interfaces in Clojure. It happens to be built on Swing, but please don't hold that against it.

## Features

Seesaw is compatible with Clojure 1.4, but will probably work fine with 1.3 and 1.5. Maybe even 1.2.

* Swing knowledge is *not required* for many apps!
* [Construct widgets](https://github.com/daveray/seesaw/wiki/Widgets) with simple functions, e.g. `(listbox :model (range 100))`
* Support for all of Swing's built-in widgets as well as SwingX.
* Support for all of Swing's layout managers as well as MigLayout, and JGoodies Forms
* Convenient shortcuts for most properties. For example, `:background :blue` or `:background "#00f"`, or `:size [640 :by 480]`.
* [CSS-style selectors](https://github.com/daveray/seesaw/wiki/Selectors) with same syntax as [Enlive](https://github.com/cgrand/enlive).
* Unified, extensible [event API](https://github.com/daveray/seesaw/wiki/Handling-events)
* Unified, extensible [selection API](https://github.com/daveray/seesaw/wiki/Handling-selection)
* [Widget binding](http://blog.darevay.com/2011/07/seesaw-widget-binding/), i.e. map changes from one widget into one or more others in a more functional style. Also integrates with Clojure's reference types.
* [Graphics](https://github.com/daveray/seesaw/wiki/Graphics)
* [i18n](https://github.com/daveray/seesaw/wiki/Resource-bundles-and-i18n)
* An extensive [test suite](https://github.com/daveray/seesaw/tree/master/test/seesaw/test)

_There are numerous Seesaw examples in [test/seesaw/test/examples](https://github.com/daveray/seesaw/tree/master/test/seesaw/test/examples)._

## TL;DR

Here's how you use Seesaw with [Leiningen](https://github.com/technomancy/leiningen)

Install `lein` as described and then:

    $ lein new hello-seesaw
    $ cd hello-seesaw

Add Seesaw to `project.clj`

```clojure
(defproject hello-seesaw "1.0.0-SNAPSHOT"
  :description "FIXME: write"
  :dependencies [[org.clojure/clojure "1.4.0"]
                [seesaw "x.y.z"]])
```

_Replace the Seesaw version with whatever the latest version tag is. See below!_

Now edit the generated `src/hello_seesaw/core.clj` file:

```clojure
(ns hello-seesaw.core
  (:use seesaw.core))

(defn -main [& args]
  (invoke-later
    (-> (frame :title "Hello",
           :content "Hello, Seesaw",
           :on-close :exit)
     pack!
     show!)))
```

Now run it:

    $ lein run -m hello-seesaw.core

*NOTE:* Here's how you can run against the bleeding edge of Seesaw:

* Clone Seesaw from github. Fork if you like. *Switch to the "develop" branch.*
* In your Seesaw checkout, run `lein install` to build it. *Note that Seesaw uses Leiningen 2 as of 3 NOV 2012!*
* In your project's `project.clj` file, change the Seesaw version to `X.Y.Z-SNAPSHOT` to match whatever's in Seesaw's `project.clj`.
* Run `lein deps` ... actually you can just start coding. `lein deps` is almost never necessary.
* Move along

## Contributors

* Meikel Brandmeyer (kotarak)
* David Brunell (Quantalume)
* Stuart Campbell (harto)
* Michael Frericks
* Jonathan Fischer Friberg (odyssomay)
* Anthony Grimes (Raynes)
* Thomas Karolski (MHOOO)
* Chun-wei Kuo (Domon)
* Vladimir Matveev (dpx-infinity)
* Jeff Rose (rosejn)
* Simon Lundmark (simlun)
* Jake McCrary (jakemcc)

## License

Copyright (C) 2012 Dave Ray

Distributed under the Eclipse Public License, the same as Clojure.

