;   Copyright (c) Christophe Grand, 2009. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.selector
  "Seesaw selector support, based largely upon enlive-html.
  https://github.com/cgrand/enlive

  There's no need to ever directly require this namespace. Use (seesaw.core/select)!"
  (:require [seesaw.util :as ssu])
  (:require [clojure.zip :as z]))

; This code is the HTML selector code for Enlive with modifications to support
; selecting from a Swing widget hierarchy. Everything's been pretty much
; locked down because other than the (select) function, I don't know what
; I want to expose yet.

(defprotocol Selectable
  (id-of* [this])
  (id-of!* [this id])
  (class-of* [this])
  (class-of!* [this classes]))

(defprotocol Tag
  (tag-name [this]))

(defn id-of
  "Retrieve the id of a widget. Use (seesaw.core/id-of)."
  [w]
  (id-of* w))

(defn id-of!
  "INTERNAL USE ONLY."
  [w id]
  (let [existing-id (id-of w)]
    (when existing-id (throw (IllegalStateException. (str ":id is already set to " existing-id))))
    ; TODO should we enforce unique ids?
    (id-of!* w id)))

(defn class-of
  "Retrieve the classes of a widget as a set of strings"
  [w]
  (class-of* w))

(defn class-of!
  "INTERNAL USE ONLY."
  [w classes]
  (class-of!* w classes))

(defn id-selector? [s]
  (.startsWith (name s) "#"))

(defn- mapknit
 ([f coll]
   (mapknit f coll nil))
 ([f coll etc]
  (lazy-seq
    (if (seq coll)
      (f (first coll) (mapknit f (rest coll) etc))
      etc))))

(defn- iterate-while
 ([f x]
  (lazy-seq (when x (cons x (iterate-while f (f x))))))
 ([f x pred]
  (lazy-seq (when (pred x) (cons x (iterate-while f (f x) pred))))))


;; utilities

(defn- node? [x]
  true)
  ;(or (string? x) (map? x)))

(defn- as-nodes [node-or-nodes]
  (if (node? node-or-nodes)
    [node-or-nodes]
    node-or-nodes))

(defn- flatten-nodes-coll [x]
  (letfn [(flat* [x stack]
            (if (node? x)
              (cons x (when (seq stack) (flat (peek stack) (pop stack))))
              (if-let [[x & xs] (seq x)]
                (recur x (conj stack xs))
                (when (seq stack)
                  (recur (peek stack) (pop stack))))))
          (flat [x stack]
            (lazy-seq (flat* x stack)))]
    (flat x ())))

(defn- flatmap [f node-or-nodes]
  (flatten-nodes-coll (map f (as-nodes node-or-nodes))))

(defn- attr-values
 "Returns the whitespace-separated values of the specified attr as a set or nil."
 [node attr]
  (when-let [v (-> node :attrs (get attr))]
    (set (re-seq #"\S+" v))))

(defn- swing-zipper
  [root]
  (z/zipper (constantly true) ssu/children identity root))

;; predicates utils
(defn- zip-pred
 "Turns a predicate function on elements locs into a predicate-step usable in selectors."
 [f]
  #(and (z/branch? %) (f %)))

(defn- pred
 "Turns a predicate function on elements into a predicate-step usable in selectors."
 [f]
  (zip-pred #(f (z/node %))))

(defn- text-pred
 "Turns a predicate function on strings (text nodes) into a predicate-step usable in selectors."
 [f]
  #(let [n (z/node %)] (and (string? n) (f n))))

(defn- re-pred
 "Turns a predicate function on strings (text nodes) into a predicate-step usable in selectors."
 [re]
  (text-pred #(re-matches re %)))

(def ^{:private true} whitespace (re-pred #"\s*"))

;; core predicates
(def ^{:private true} any (pred (constantly true)))

(defn- tag=
 "Selector predicate, :foo is as short-hand for (tag= :foo)."
 [expected-tag-name]
  (pred
    (fn [v]
        (= (if (satisfies? Tag v)
             (tag-name v)
             (.getSimpleName (class v)))
           expected-tag-name))))

(defn- id=
 "Selector predicate, :#foo is as short-hand for (id= \"foo\")."
 [id]
  (pred #(= (-> % id-of) (keyword id))))

(defn- exact-type=
  [class-name]
  (let [cls (Class/forName class-name)]
    (pred #(do (= (class %) cls) ))))

(defn- loose-type=
  [class-name]
  (let [cls (Class/forName class-name)]
    (pred #(.isInstance cls %))))

(defn- attr-has
 "Selector predicate, tests if the specified whitespace-seperated attribute contains the specified values. See CSS ~="
 [attr & values]
  (pred #(when-let [v (attr-values % attr)] (every? v values))))

(defn- has-class
 "Selector predicate, :.foo.bar. Looks for widgets with (:class #{:foo :bar})"
 [& classes]
 (pred #(when-let [v (class-of %)] (every? v classes))))

;; selector syntax
(defn- intersection [preds]
  (condp = (count preds)
    1 (first preds)
    2 (let [[f g] preds] #(and (f %) (g %)))
    3 (let [[f g h] preds] #(and (f %) (g %) (h %)))
    4 (let [[f g h k] preds] #(and (f %) (g %) (h %) (k %)))
    (fn [x] (every? #(% x) preds))))

(defn- union [preds]
  (condp = (count preds)
    1 (first preds)
    2 (let [[f g] preds] #(or (f %) (g %)))
    3 (let [[f g h] preds] #(or (f %) (g %) (h %)))
    4 (let [[f g h k] preds] #(or (f %) (g %) (h %) (k %)))
    (fn [x] (some #(% x) preds))))


(def ^{:private true} segment-regex #"^<([\w.!]+)>(.*)")
(defn- split-segments
  [^String s]
  (if-let [[_ ^String class-name & more]  (re-matches segment-regex s)]
    (if (.endsWith class-name "!")
      (cons (str "+" (subs class-name 0 (dec (count class-name)))) (remove empty? more))
      (cons (str "*" class-name) (remove empty? more)))
    (seq (.split s "(?=[#.])"))))

(def ^{:private true} compile-keyword
  (memoize
    (fn [kw]
      (if (= :> kw)
        :>
        (let [[[first-letter :as tag-name] :as segments] (split-segments (name kw))
              classes (for [s segments :when (= \. (first s))] (subs s 1))
              preds (when (seq classes) (list (apply has-class classes)))
              preds (if (contains? #{nil \* \# \. \+} first-letter)
                      preds
                      (conj preds (tag= tag-name)))
              preds (reduce (fn [preds [x :as segment]]
                              (if (= \# x)
                                (conj preds (id= (subs segment 1)))
                                (if (= \+ x)
                                  (conj preds (exact-type= (subs segment 1)))
                                  (if (and (= \* x) (> (count segment) 1))
                                    (conj preds (loose-type= (subs segment 1)))
                                    preds)))) preds segments)]
         (if (seq preds) (intersection preds) any))))))

(defn- compile-step [step]
  (cond
    (string? step) (compile-keyword (keyword step))
    (keyword? step) (compile-keyword step)
    (set? step) (union (map compile-step step))
    (vector? step) (intersection (map compile-step step))
    :else step))

(defn- compile-chain [chain]
  (map compile-step chain))

(defn- selector-chains [selector id]
  (for [x (tree-seq set? seq selector) :when (not (set? x))]
    (compile-chain (concat x [id]))))

(defn- predset [preds]
  (condp = (count preds)
    1 (let [[f] preds] #(if (f %) 1 0))
    2 (let [[f g] preds] #(+ (if (f %) 1 0) (if (g %) 2 0)))
    3 (let [[f g h] preds] #(-> (if (f %) 1 0) (+ (if (g %) 2 0))
                              (+ (if (h %) 4 0))))
    4 (let [[f g h k] preds] #(-> (if (f %) 1 0) (+ (if (g %) 2 0))
                                (+ (if (h %) 4 0)) (+ (if (k %) 8 0))))
    #(loop [i 1 r 0 preds (seq preds)]
       (if-let [[pred & preds] preds]
         (recur (bit-shift-left i 1) (if (pred %) (+ i r) r) preds)
         r))))

(defn- states [init chains-seq]
  (fn [^Number n]
    (loop [n n s (set init) [chains & etc] chains-seq]
      (cond
        (odd? n) (recur (bit-shift-right n 1) (into s chains) etc)
        (zero? n) s
        :else (recur (bit-shift-right n 1) s etc)))))

(defn- make-state [chains]
  (let [derivations
          (reduce
            (fn [derivations chain]
              (cond
                (= :> (first chain))
                  (let [pred (second chain)]
                    (assoc derivations pred (conj (derivations pred) (nnext chain))))
                (next chain)
                  (let [pred (first chain)]
                    (-> derivations
                      (assoc nil (conj (derivations nil) chain))
                      (assoc pred (conj (derivations pred) (next chain)))))
                :else
                  (assoc derivations :accepts (first chain)))) {} chains)
        always (derivations nil)
        accepts (derivations :accepts)
        derivations (dissoc derivations nil :accepts)
        ps (predset (keys derivations))
        next-states (memoize #(make-state ((states always (vals derivations)) %)))]
    [accepts (when (seq chains) (comp next-states ps))]))

(defn cacheable [selector] (vary-meta selector assoc ::cacheable true))
(defn cacheable? [selector] (-> selector meta ::cacheable))

(defn- automaton* [selector]
  (make-state (-> selector (selector-chains 0) set)))

(defn- lockstep-automaton* [selectors]
  (make-state (set (mapcat selector-chains selectors (iterate inc 0)))))

(def ^{:private true} memoized-automaton* (memoize automaton*))

(def ^{:private true} memoized-lockstep-automaton* (memoize lockstep-automaton*))

(defn- automaton [selector]
  ((if (cacheable? selector) memoized-automaton* automaton*) selector))

(defn- lockstep-automaton [selectors]
  ((if (every? cacheable? selectors) memoized-lockstep-automaton* lockstep-automaton*) selectors))

(defn- accept-key [s] (nth s 0))
(defn- step [s x] (when-let [f (and s (nth s 1))] (f x)))

(defn- fragment-selector? [selector]
  (map? selector))

(defn- node-selector? [selector]
  (not (fragment-selector? selector)))

(defn- static-selector? [selector]
  (or (keyword? selector)
    (and (coll? selector) (every? static-selector? selector))))

;; core

(defn- children-locs [loc]
  (iterate-while z/right (z/down loc)))

(defn- zip-select-nodes* [locs state]
  (letfn [(select1 [loc previous-state]
            (when-let [state (step previous-state loc)]
              (let [descendants (mapcat #(select1 % state) (children-locs loc))]
                (if (accept-key state) (cons loc descendants) descendants))))]
    (mapcat #(select1 % state) locs)))

(defn- select-nodes* [nodes selector]
  (let [state (automaton selector)]
    (map z/node (zip-select-nodes* (map swing-zipper nodes) state))))

(defn- zip-select-fragments* [locs state-from state-to]
  (letfn [(select1 [locs previous-state-from previous-state-to]
            (when (and previous-state-from previous-state-to)
              (let [states-from (map #(step previous-state-from %) locs)
                    states-to (map #(step previous-state-to %) locs)
                    descendants (reduce into []
                                  (map #(select1 (children-locs %1) %2 %3)
                                    locs states-from states-to))]
                (loop [fragments descendants fragment nil
                       locs locs states-from states-from states-to states-to]
                  (if-let [[loc & etc] (seq locs)]
                    (if fragment
                      (let [fragment (conj fragment loc)]
                        (if (accept-key (first states-to))
                          (recur (conj fragments fragment) nil etc
                            (rest states-from) (rest states-to))
                          (recur fragments fragment etc
                            (rest states-from) (rest states-to))))
                      (if (accept-key (first states-from))
                        (recur fragments [] locs states-from states-to)
                        (recur fragments nil etc
                          (rest states-from) (rest states-to))))
                    fragments)))))]
    (select1 locs state-from state-to)))

(defn- select-fragments* [nodes selector]
  (let [[selector-from selector-to] (first selector)
        state-from (automaton selector-from)
        state-to (automaton selector-to)]
    (map #(map z/node %)
      (zip-select-fragments* (map swing-zipper nodes) state-from state-to))))

(defn select
 "*USE seesaw.core/select*
  Returns the seq of nodes or fragments matched by the specified selector."
 [node-or-nodes selector]
  (let [nodes (as-nodes node-or-nodes)]
    (if (node-selector? selector)
      (select-nodes* nodes selector)
      (select-fragments* nodes selector))))

(defn- zip-select
 "Returns the seq of locs matched by the specified selector."
 [locs selector]
  (if (node-selector? selector)
    (apply zip-select-nodes* locs selector)
    (apply zip-select-fragments* locs selector)))

;; other predicates
(defn- attr?
 "Selector predicate, tests if the specified attributes are present."
 [& kws]
  (pred #(every? (-> % :attrs keys set) kws)))

(defn- every?+ [pred & colls]
  (every? #(apply pred %) (apply map vector colls)))

(defn- multi-attr-pred
 [single-attr-pred]
  (fn [& kvs]
    (let [ks (take-nth 2 kvs)
          vs (take-nth 2 (rest kvs))]
      (pred #(when-let [attrs (:attrs %)]
               (every?+ single-attr-pred (map attrs ks) vs))))))

(def ^{:private true
       :doc "Selector predicate, tests if the specified attributes have the specified values."}
 attr=
  (multi-attr-pred =))

(defn- starts-with? [^String s ^String prefix]
  (and s (.startsWith s prefix)))

(defn- ends-with? [^String s ^String suffix]
  (and s (.endsWith s suffix)))

(defn- contains-substring? [^String s ^String substring]
  (and s (<= 0 (.indexOf s substring))))

(def ^{:private true
       :doc "Selector predicate, tests if the specified attributes start with the specified values. See CSS ^= ."}
 attr-starts
  (multi-attr-pred starts-with?))

(def ^{:private true
       :doc "Selector predicate, tests if the specified attributes end with the specified values. See CSS $= ."}
 attr-ends
  (multi-attr-pred ends-with?))

(def ^{:private true
       :doc "Selector predicate, tests if the specified attributes contain the specified values. See CSS *= ."}
 attr-contains
  (multi-attr-pred contains-substring?))

(defn- is-first-segment? [^String s ^String segment]
  (and s
    (.startsWith s segment)
    (= \- (.charAt s (count segment)))))

(def ^{:private true
       :doc "Selector predicate, tests if the specified attributes start with the specified values. See CSS |= ."}
 attr|=
  (multi-attr-pred is-first-segment?))

(def ^{:private true} root
  (zip-pred #(-> % z/up nil?)))

(defn- nth?
 [f a b]
  (if (zero? a)
    ;#(= (-> (filter xml/tag? (f %)) count inc) b)
    #(= (-> (filter (constantly true) (f %)) count inc) b)
    ;#(let [an+b (-> (filter xml/tag? (f %)) count inc)
    #(let [an+b (-> (filter (constantly true) (f %)) count inc)
           an (- an+b b)]
       (and (zero? (rem an a)) (<= 0 (quot an a))))))

(defn- nth-child
 "Selector step, tests if the node has an+b-1 siblings on its left. See CSS :nth-child."
 ([b] (nth-child 0 b))
 ([a b] (zip-pred (nth? z/lefts a b))))

(defn- nth-last-child
 "Selector step, tests if the node has an+b-1 siblings on its right. See CSS :nth-last-child."
 ([b] (nth-last-child 0 b))
 ([a b] (zip-pred (nth? z/rights a b))))

(defn- filter-of-type [f]
  (fn [loc]
    (let [tag (-> loc z/node :tag)
          pred #(= (:tag %) tag)]
      (filter pred (f loc)))))

(defn- nth-of-type
 "Selector step, tests if the node has an+b-1 siblings of the same type (tag name) on its left. See CSS :nth-of-type."
 ([b] (nth-of-type 0 b))
 ([a b] (zip-pred (nth? (filter-of-type z/lefts) a b))))

(defn- nth-last-of-type
 "Selector step, tests if the node has an+b-1 siblings of the same type (tag name) on its right. See CSS :nth-last-of-type."
 ([b] (nth-last-of-type 0 b))
 ([a b] (zip-pred (nth? (filter-of-type z/rights) a b))))

(def ^{:private true} first-child (nth-child 1))

(def ^{:private true} last-child (nth-last-child 1))

(def ^{:private true} first-of-type (nth-of-type 1))

(def ^{:private true} last-of-type (nth-last-of-type 1))

(def ^{:private true} only-child (intersection [first-child last-child]))

(def ^{:private true} only-of-type (intersection [first-of-type last-of-type]))

(def ^{:private true} void (pred #(empty? (remove empty? (:content %)))))

(def ^{:private true} odd (nth-child 2 1))

(def ^{:private true} even (nth-child 2 0))

(defn- select? [node-or-nodes selector]
  (-> node-or-nodes as-nodes (select selector) seq boolean))

(defn- has
 "Selector predicate, matches elements which contain at least one element that
  matches the specified selector. See jQuery's :has"
 [selector]
  (pred #(select? (:content %) selector)))

(defn- but-node
 "Selector predicate, matches nodes which are rejected by the specified selector-step. See CSS :not"
 [selector-step]
  (complement (compile-step selector-step)))

(defn- but
 "Selector predicate, matches elements which are rejected by the specified selector-step. See CSS :not"
 [selector-step]
  (intersection [any (but-node selector-step)]))

(defn- left [selector-step]
  (let [selector [:> selector-step]]
    ;#(when-let [sibling (first (filter xml/tag? (reverse (z/lefts %))))]
    #(when-let [sibling (first (filter (constantly true) (reverse (z/lefts %))))]
       (select? sibling selector))))

(defn- lefts [selector-step]
  (let [selector [:> selector-step]]
    ;#(select? (filter xml/tag? (z/lefts %)) selector)))
    #(select? (filter (constantly true) (z/lefts %)) selector)))

(defn- right [selector-step]
  (let [selector [:> selector-step]]
    ;#(when-let [sibling (first (filter xml/tag? (z/rights %)))]
    #(when-let [sibling (first (filter (constantly true) (z/rights %)))]
       (select? sibling selector))))

(defn- rights [selector-step]
  (let [selector [:> selector-step]]
    ;#(select? (filter xml/tag? (z/rights %)) selector)))
    #(select? (filter (constantly true) (z/rights %)) selector)))

(def ^{:private true} any-node (constantly true))

(def ^{:private true} this-node [:> any-node])

(def ^{:private true} text-node #(string? (z/node %)))

;; screen-scraping utils
(defn- text
 "Returns the text value of a node."
 {:tag String}
 [node]
  (cond
    (string? node) node
    ;(xml/tag? node) (apply str (map text (:content node)))
    :else ""))

(defn- texts
 "Returns the text value of a nodes collection."
 {:tag String}
 [nodes]
  (map text nodes))

;(defmacro let-select
 ;"For each node or fragment, performs a subselect and bind it to a local,
  ;then evaluates body.
  ;bindings is a vector of binding forms and selectors."
 ;[nodes-or-fragments bindings & body]
  ;(let [node-or-fragment (gensym "node-or-fragment__")
        ;bindings
          ;(map (fn [f x] (f x))
            ;(cycle [identity (fn [spec] `(select ~node-or-fragment ~spec))])
            ;bindings)]
    ;`(map (fn [~node-or-fragment]
            ;(let [~@bindings]
              ;~@body)) ~nodes-or-fragments)))

