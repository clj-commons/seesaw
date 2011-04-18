# Seesaw: a Clojure/Swing experiment

Seesaw's a *primordial* experiment to see what I can do to make Swing funner in Clojure. It's maybe inspired by [Shoes](http://shoesrb.com/), [Stuart Sierra's Swing posts](http://stuartsierra.com/tag/swing), etc. [clojure.contrib.swing-utils](http://richhickey.github.com/clojure-contrib/swing-utils-api.html) is useful, but minimal and still means a lot of "Java-in-Clojure" coding.

## TODO

* GridBagLayout
* JTree
* Cell renderers
* Graphics
* Structural manip
* Selectors
* Styling

## Usage
See tests and src/seesaw/examples. Seriously, the tests are pretty descriptive of how things work.

Let's create a `JFrame`:

    (frame :title "Hello" :content "Hi there")

This will create a `JFrame` with title "Hello" and a single label "Hi there". The `:content` property expects something that can be turned into a widget and uses it as the content pane of the frame. Any place where a widget is expected, one will be created depending on the argument...

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
                (action choose :name "...")]))

creates a panel with a "File" label, a text entry field initialized to the current working directory and a button that doesn't do much. Each component is separated by 5 pixel padding.

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
</table>

... and several more. 

Note that these properties can also be used with the `(config)` function which applies them to an existing widget or widgets:

    (config (select :#my-widget) :enabled? false :text "I'm disabled.")

### Selectors

I hope to one day support general CSS-style selectors for finding and styling widgets in an app. For now, the `(select)` function supports locating a widget by `:id` as set at creation time:

    (button :id :the-button :text "Push me")

       ... later ...

    (listen (select :#the-button) 
      :action (fn [e] ... do something ...))


At the moment, I'm planning on following the selector conventions established by [Enlive] (https://github.com/cgrand/enlive). See also, the apparrently defunct [Java CSS] (http://weblogs.java.net/blog/2008/07/17/introducing-java-css) project to get an idea where this may lead.

### Containers

There are container creation functions which basically create `JPanel` instances with particular layouts. Here are some examples. Any place that a widget or list of widgets is expected, the widget coercion rules described above apply.

A `FlowLayout` with some items:

    (flow-panel 
       :align :left
       :hgap 20
       :items ["Label" (action alert "Button") "Another label"])

A `GridLayout` with 2 columns and a titled border:

    (grid-panel
      :border "Properties"
      :columns 2
      :items ["Name" (text "Frank") 
              "Address" (text "123 Main St")])

A `BorderLayout` with labels at each position:
    
    (border-panel :hgap 10 :vgap 10 :center "CENTER" :north "NORTH" :south "SOUTH" :east "EAST" :west "WEST")

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

See `seesaw.events/add-listener` for more details.
    
### Actions
It's typical in Swing apps to use actions for menus, buttons, etc. An action needs an event handler function and some properties. Here's an example of creating an action and adding it to a toolbar:

    (use 'seesaw.core)
    (let [open-action (action (fn [e] (alert "I should open a new something."))
                        :name "Open"
                        :tip  "Open a new something something.")
          exit-action (action (fn [e] (.dispose (to-frame e)))
                        :name "Exit"
                        :tip  "Close this window")]
      (frame 
        :title "Toolbar action test"
        :content (border-panel
                    :north (toolbar :items [open-action exit-action])
                    :center "Insert content here")))

`(action)` also supports an `:icon` property which can be a `javax.swing.Icon`, a `java.net.URL` or something that looks like a file or URL after `(str)` has been applied to it.

### Selection Handling
The `(selection)` function handles the details of selection management for listboxes, checkboxes, toggle buttons, combo boxes, etc. To get the current selection, just pass a widget (or something convertible to a widget). It will always return a seq of values, or `nil` if there is no selection. For single-selection cases, just use `(first)`. Note that you can apply `(selection)` to event objects as well:

    (listen (select :#my-list) :selection
      (fn [e]
       (println "Current selection of my-list is: " (selection e))))

Giving an argument to the `(selection)` function will set the current selection:

    (let [my-list (listbox :model ["jim" "bob" "al"])]
      (selection my-list ["bob"]))

Pass `nil` to clear the selection.

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

## License

Copyright (C) 2011 Dave Ray

Distributed under the Eclipse Public License, the same as Clojure.
