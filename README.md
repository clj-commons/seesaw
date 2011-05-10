# Seesaw: a Clojure/Swing experiment

_Seesaw's experimental and subject to radical change_

Seesaw's an experiment to see what I can do to make Swing funner in Clojure. It's kinda inspired by [Shoes](http://shoesrb.com/), [Stuart Sierra's Swing posts](http://stuartsierra.com/tag/swing), etc. [clojure.contrib.swing-utils](http://richhickey.github.com/clojure-contrib/swing-utils-api.html) is useful, but minimal and still means a lot of "Java-in-Clojure" coding.

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
                    [seesaw "1.0.0"]])

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

## Usage
See tests and src/seesaw/examples. Seriously, there are a lot of tests and they're pretty descriptive of how things work.

Let's create a `JFrame`:

    (frame :title "Hello" :content "Hi there")

This will create a `JFrame` with title "Hello" and a single label "Hi there". The `:content` property expects something that can be turned into a widget and uses it as the content pane of the frame. Any place where a widget is expected, one will be created depending on the argument (see Widget Coercion below) ...

There are several examples at the moment. They're all in the `src/seesaw/examples` and can be run with `lein` like this:

    $ lein deps
    $ lein run -m seesaw.examples.<name-of-example>

To run the tests:

    $ java -cp "src:test:classes:lib/*:lib/dev/*" lazytest.watch src test 

    Hopefully you see a nice wall of green.

### Widget Coercion

<table>
  <tr><td>Input</td><td>Result</td></tr>
  <tr><td>java.awt.Component</td><td>return argument unchanged</td></tr>
  <tr><td>java.awt.Dimension</td><td>return Box/createRigidArea</td></tr>
  <tr><td>java.swing.Action</td><td>return a button using the action</td></tr>
  <tr><td>java.util.EventObject (for example in an event handler)</td><td>return the event source</td></tr>
  <tr><td>:fill-h</td><td>Box/createHorizontalGlue</td></tr>
  <tr><td>:fill-v</td><td>Box/createVerticalGlue</td></tr>
  <tr><td>[:fill-h n], e.g. <code>[:fill-h 99]<code></td><td>Box/createHorizontalStrut with width n</td></tr>
  <tr><td>[:fill-v n]</td><td>Box/createVerticalStrut with height n</td></tr>
  <tr><td>[width :by height]</td><td>create rigid area with given dimensions</td></tr>
  <tr><td>A URL</td><td>a label with the image located at the url</td></tr>
  <tr><td>A non-url string</td><td>a label with the given text</td></tr>
</table>


Most of Seesaw's container functions (`flow-panel`, `grid-panel`, etc) take an `:items` property which is a list of these widget-able values. For example:

    (let [choose (fn [e] (alert "I should open a file chooser"))]
      (flow-panel
        :items ["File"                                 [:fill-h 5] 
                (text (System/getProperty "user.dir")) [:fill-h 5] 
                (action :handler choose :name "...")]))

creates a panel with a "File" label, a text entry field initialized to the current working directory and a button that doesn't do much. Each component is separated by 5 pixel padding.

New coercions can be added by extending the `ToWidget` protocol. See the `to-widget` example.

### Default Properties
All of Seesaw's widget creation functions (`label`, `text`, `horizontal-panel`, etc) support a base set of properties:

<table>
  <tr><td>Property</td><td>Description</td></tr>
  <tr><td><code>:id</code></td><td>A unique id for the widget for use with `(select)` (see below).</td></tr>
  <tr><td><code>:opaque</code></td><td>(boolean) Set whether the background of the widget is opaque.</td></tr>
  <tr><td><code>:background</code></td><td>Background color by coercing into a Color (see below)</td></tr>
  <tr><td><code>:foreground</code></td><td>Foreground color by coercing into a Color (see below)</td></tr>
  <tr><td><code>:border</code></td><td>Set the border of the widget by coercing into a Border. See below.</td></tr>
  <tr><td><code>:font</code></td><td>Set the font of the widget by coercing into a Font. See below.</td></tr>
  <tr><td><code>:enabled?</code></td><td>Whether the widget is enabled or not.</td></tr>
  <tr><td><code>:listen</code></td><td>List of event listeners with same format as args to `(listen)` function (see below).</td></tr>
  <tr><td><code>:minimum-size</code></td><td>Minimum size of component, set with a `java.awt.Dimension` or a vector of the form `[width :by height]`, for example `[50 :by 50]`. Note that in Swing, some containers don't honor minimum size.</td></tr>
  <tr><td><code>:maximum-size</code></td><td>Same as `:minimum-size`, but maximum. Note that in Swing, some containers don't honor maximum size.</td></tr>
  <tr><td><code>:preferred-size</code></td><td>Same as `:minimum-size`, but preferred size.</td></tr>
  <tr><td><code>:size</code></td><td>Set `:minimum-size`, `:maximum-size`, and `:preferred-size` all at once.</td></tr>
  <tr><td><code>:popup</code></td><td>A JPopupMenu or function that generates the items of a popup menu to display. In this case, Seesaw ensures that the popup is shown on the correct mouse event for the platform. See `src/seesaw/examples/popup.clj`</td></tr>
</table>

... and many more. See code and tests for details. 

Note that these properties can also be used with the `(config!)` function which applies them to an existing widget or widgets:

    (config! (select [:#my-widget]) :enabled? false :text "I'm disabled.")

`(config!)` can be applied to a single widget, or list of widgets, or things that can be turned into widgets.

### Selectors

I hope to one day support general CSS-style selectors for finding and styling widgets in an app. For now, the `(select)` function supports locating a widget by `:id` as set at creation time:

    (button :id :the-button :text "Push me")

       ... later ...

    (listen (select [:#the-button]) 
      :action (fn [e] ... do something ...))

_I'm not totally happy with this and it will probably change. Selecting by id becomes ambiguous if there's more than one root frame in the app, especially if it's a second instance of the same frame type. I'll probably chnage this to take a root in addition to the selector unless I think of something better._

The "all" selector is also supported which will match everything in a sub-tree including the root. For example to disable an entire sub-tree:

    (config! (select [:*] my-panel) :enabled? false)

At the moment, I'm planning on following the selector conventions established by [Enlive] (https://github.com/cgrand/enlive). See also, the apparently defunct [Java CSS] (http://weblogs.java.net/blog/2008/07/17/introducing-java-css) project to get an idea where this may lead.

_Of all the areas of Seesaw, I think this is the least stable. You may be best off just binding widgets to variables and passing them around_

### Creating a Frame

A typical Swing app will stick everything inside an instance of [JFrame] (http://download.oracle.com/javase/6/docs/api/javax/swing/JFrame.html). Create a `JFrame` in Seesaw with the `(frame)` function: 

    (frame :title "An example", :on-close :exit, :content "Some Content")

`(frame)` takes an number of options (see the code and tests), but two very important options are `:on-close` and `:content`.

`:on-close` sets the default behavior when the frame is closed by the user. Valid values are `:exit`, `:hide`, `:dispose`, and `:nothing`. Note that `:exit` will cause the entire JVM to exit when the frame is closed, which may be reasonable for a standalone app, but probably isn't for use in the REPL. The default value is `:hide`.

`:content` sets the content of the frame which can be any widget (see Widget Coercion above), but is usually a panel of some sort.

### A Note on Threading

As noted [here] (http://download.oracle.com/javase/6/docs/api/javax/swing/package-summary.html#threading) Swing is single threaded nearly all UI operations should be executed on the Swing UI dispatch thread. To facilitate this, Seesaw includes the `(invoke-now)` and `(invoke-later)` macros. The former executes forms on the UI thread and waits for their completion, while the latter simply schedules the forms for execution sometime in the future.

A typical use for `(invoke-later)` is to get things going in an app:

    (defn -main [& args]
      (invoke-later
        (frame :title "Hello" :content (button :text "Push me"))))

### Containers

There are container creation functions which basically create `JPanel` instances with particular layouts. Here are some examples. Any place that a widget or list of widgets is expected, the widget coercion rules described above apply.

A `FlowLayout` with some items:

    (flow-panel 
       :align :left
       :hgap 20
       :items ["Label" (action :handler alert "Button") "Another label"])

A `GridLayout` with 2 columns and a titled border:

    (grid-panel
      :border "Properties"
      :columns 2
      :items ["Name" (text "Frank") 
              "Address" (text "123 Main St")])

A `BorderLayout` with labels at each position:
    
    (border-panel :hgap 10 :vgap 10 :center "CENTER" :north "NORTH" :south "SOUTH" :east "EAST" :west "WEST")

There's also `(mig-panel)` which uses [MigLayout] (http://www.miglayout.com/), `(vertical-panel)`, `(horizontal-panel)`, `(border-panel)`, etc.

### Event Handlers
Event handler functions are single-argument functions that take an event object whose type depends on the event being fired, e.g. `MouseEvent`. For example, we can execute a function when a checkbox is checked:

    (let [handler (fn [e] (alert (.. (.getSource e) (isSelected))))]
      (checkbox :text "Check me" 
        :listen [:item-state-changed handler]))

Event handlers are installed with the `(listen)` function. Its first argument is a widget, or seq of widgets, and then one or more event specs of the form event-name/function. For the most part, the name of the event is the name of a Swing event listener method. Here's an example that listens for some mouse events, assuming that `p` is bound to a widget:

    (listen p
      :mouse-clicked (fn [e] ... do something ...)
      :mouse-entered (fn [e] ... do something ...)
      :mouse-exited  (fn [e] ... do something ...))

Note that these same arguments can be given to the `:listen` property when the widget is constructed.

`(listen)` returns a function which, when called, will remove all listeners installed by the `(listen)` call. There is no "remove-listener" function.

See `seesaw.events/listen` for more details.
    
### Actions
It's typical in Swing apps to use actions for menus, buttons, etc. An action needs a handler function and some properties. Here's an example of creating an action and adding it to a toolbar:

    (use 'seesaw.core)
    (let [open-action (action 
                        :handler (fn [e] (alert "I should open a new something."))
                        :name "Open"
                        :tip  "Open a new something something.")
          exit-action (action 
                        :handler (fn [e] (.dispose (to-frame e)))
                        :name "Exit"
                        :tip  "Close this window")]
      (frame 
        :title "Toolbar action test"
        :content (border-panel
                    :north (toolbar :items [open-action exit-action])
                    :center "Insert content here")))

`(action)` also supports an `:icon` property which can be a `javax.swing.Icon`, a `java.net.URL` or something that looks like a file or URL after `(str)` has been applied to it. See `seesaw/action.clj` for an accurate list of options.

Like widgets, actions can be modified with the `(config!)` function:

    (def a (action :name "Fire Missiles" :enabled? false))

    (config! a :name "Fire Missiles!!!" :enabled? true :handler (fn [e] (println "FIRE")))

### Menus
Here's how you can make a menu bar full of menus:

    (frame :title "MENUS!"
      :menubar 
        (menubar :items 
          [(menu :text "File" :items [new-action open-action save-action exit-action])
           (menu :text "Edit" :items [copy-action paste-action])))

`(menubar)` has a list of `(menus)`, while each `(menu)` has text and a list of actions, or items. Note that in addition to using Actions as menu items, you can also use `(menu-item)`, `(checkbox-menu-item)`, and `(radio-menu-item)`, each of which has the exact same behavior (and options) as a button.

Popup menus (context/right-click) can be easily added to a widget with the `:popup` property.  Just give it a single-argument function that returns a list of actions or menu items. The function will be called each time the menu needs to be displayed. For example,

      (listbox :popup (fn [e] [action1 action2 ...]))

Seesaw takes care of registering the mouse handler and showing the popup at the right time depending on the platform. See `src/seesaw/examples/popup.clj`.

### Selection Handling
The `(selection)` and `(selection!)` function handles the details of selection management for listboxes, checkboxes, toggle buttons, combo boxes, etc. To get the current selection, just pass a widget (or something convertible to a widget) to `(selection`). It will always return the selected value, or `nil` if there is no selection:

    (if-let [s (selection my-widget)]
      (println "Current selection is " s)
      (println "No selection"))

For multi-selection, `(selection)` takes an options map:

    (doseq [s (selection {:multi? true} my-list)]
      (println "Selected: " s))

Note that you can apply `(selection)` to event objects as well:

    (listen (select [:#my-list]) :selection
      (fn [e]
       (println "Current selection of my-list is: " (selection e))))

The `(selection!)` function will set the current selection:

    (let [my-list (listbox :model ["jim" "bob" "al"])]
      (selection! my-list "bob"))

Pass `nil` to clear the selection. Like with `(selection)`, use the `multi?` option to interpret the new selection value as a list of values to select.

### Color Coercion

Colors can be specified in the following ways (using the `:foreground` property as an example):

    :foreground java.awt.Color/BLACK      (a raw color object)
    :foreground (color 255 255 224)       (RGB bytes)
    :foreground (color 255 255 224 128)   (RGBA bytes)
    :foreground "#FFEEDD"                 (hex color string)
    :foreground (color "#FFEEDD" 128)     (hex color string + alpha)

Here's a label with blue text and a red background:

    (label :text "Hideous"
           :opaque true
           :foreground (color 0 0 255)
           :background "#FF0000")

Of course, a raw `Color` object can also be used.

### Font Coercion

Fonts can be specified in the following ways (using the `:font` property as an example):

    :font "ARIAL-BOLD-18"                             (Swing-style font spec string)
    :font {:name "ARIAL" :style :bold :size 18}       (using a properties hash)
    :font (font :name "ARIAL" :style :bold :size 18)  (using properties with font function)

So, you could make a monospaced text area like this:

    (text :text "Type some code here" 
          :multi-line? true 
          :font {:name :monospaced :size 15})

Of course, a raw `Font` object can also be used.

### Border Coercion

Widget borders can be passed to the `:border` property to create many border styles:

    :border "Title"         (creates a plain title border)
    :border 10              (creates an empty 10 pixel border)
    :border [10 "Title" 5]  (compound empty/title/empty border)
    :border (line-border :thickness 3 :color "#FF0000")     (red, 3 pixel border)
    :border (line-border :top 5 :left 5)     (5 pixel black border on top and left)

Of course, a raw `Border` object can also be used.

### Scrolling

Use the `(scrollable)` function to make a widget scrollable:

    (scrollable (text :multi-line? true))

### Splitters

Use the `(top-bottom-split)` or `(left-right-split)` functions to make a splitter each takes two widget args:

    (top-bottom-split "Top" "Bottom")
    (left-right-split "Top" "Bottom")

### Graphics

Seesaw has some basic support for Java2D drawing, using the `(canvas)` function to create a paintable panel. See `src/seesaw/graphics.clj` and `src/seesaw/examples/canvas.clj`.

## License

Copyright (C) 2011 Dave Ray

Distributed under the Eclipse Public License, the same as Clojure.
