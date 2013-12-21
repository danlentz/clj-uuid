(ns clj-uuid.util
  (:use [clojure.core])
  (:use [clojure.pprint])
  (:use [clojure.repl :only [doc find-doc apropos]])
  (:use  [clojure.repl :as repl])
  (:require [clojure.reflect :as reflect])
  (:import (java.util UUID)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; doseq-indexed
;;
;; https://gist.github.com/4134522
;; https://twitter.com/Baranosky/status/271894356418498560
;;
;; Example:
;;
;; (doseq-indexed idx [name names]
;;   (println (str idx ". " name)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro doseq-indexed
  [index-sym [item-sym coll] & body]
  `(let [idx-atom# (atom 0)]
     (doseq [~item-sym ~coll]
       (let [~index-sym (deref idx-atom#)]
         ~@body
         (swap! idx-atom# inc)))))

;; http://stackoverflow.com/questions/4830900/how-do-i-find-the-index-of-an-item-in-a-vector

;; (defn map-indexed
;;   "Returns a lazy sequence consisting of the result of applying f to 0
;;   and the first item of coll, followed by applying f to 1 and the second
;;   item in coll, etc, until coll is exhausted. Thus function f should
;;   accept 2 arguments, index and item."
;;   {:added "1.2"}
;;   ([f coll] (map-indexed f 0 1 coll))
;;   ([f start coll] (map-indexed f start 1 coll))
;;   ([f start step coll] 
;;       (letfn [(mapi [idx coll]
;;                 (lazy-seq
;;                   (when-let [s (seq coll)]
;;                     (cons (f idx (first s)) (mapi (+ step idx) (rest s))))))]
;;         (mapi start coll))))

(defn indexed
  "Returns a lazy sequence of [index, item] pairs, where items come
  from 's' and indexes count up from zero.
  (indexed '(a b c d))  =>  ([0 a] [1 b] [2 c] [3 d])"
  [s]
  ;; (map vector (iterate inc 0) s))
  (map vector (range) s))

(defn positions
  "Returns a lazy sequence containing the positions at which pred
   is true for items in coll."
  [pred coll]
  (for [[idx elt] (indexed coll) :when (pred elt)] idx))


(defn knit
  "Takes a list of functions (f1 f2 ... fn) and returns a new function F. F takes
   a collection of size n (x1 x2 ... xn) and returns a vector [(f1 x1) (f2 x2) ... (fn xn)].
   Similar to Haskell's ***, and a nice complement to juxt (which is Haskell's &&&)."
  [& fs]
  (fn [arg-coll]
    (vec (map #(% %2) fs arg-coll))))


(defn split-vec
  "Split the given vector at the provided offsets using subvec. Supports negative offsets."
  [v & ns]
  (let [ns (map #(if (neg? %) (+ % (count v)) %) ns)]
    (lazy-seq
     (if-let [n (first ns)]
       (cons (subvec v 0 n)
             (apply split-vec
                    (subvec v n)
                    (map #(- % n) (rest ns))))
       (list v)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Namespace Docstring Introspection
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ns-docs
  "Prints docs for all public symbols in given namespace
   http://blog.twonegatives.com/post/42435179639/ns-docs
   https://gist.github.com/timvisher/4728530"
  [ns-symbol]
  (dorun 
   (map (comp #'repl/print-doc meta)
        (->> ns-symbol 
             ns-publics 
             sort 
             vals))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PROG1 but with fancy clojure name 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro returning
  "Compute a return value, then execute other forms for side effects.
  Like prog1 in common lisp, or a (do) that returns the first form."
  [value & forms]
  `(let [value# ~value]
     ~@forms
     value#))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Timing and Performance Metric
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro with-timing
  "Same as clojure.core/time but returns a vector of a the result of
   the code and the milliseconds rather than printing a string. Runs
   the code in an implicit do."
  [& body]
  `(let [start# (System/nanoTime)  ret# ~(cons 'do body)]
     [ret# (/ (double (- (System/nanoTime) start#)) 1000000.0)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pretty Printing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;  (def fmt "~:<~:@{[~25s ~30s]~:^~:@_~}~:>")
;; #'user/fmt

;; user=> (cl-format true fmt attrs)
;; ([:db/cardinality           :db.cardinality/one           ]
;;  [:db/doc                   "Documentation string for an entity."]
;;  [:db/fulltext              true                          ]
;;  [:db/ident                 :db/doc                       ]
;;  [:db/valueType             :db.type/string               ])
;; nil

;; ;;;

;; Since you're interested in pprint/cl-format, I'll deconstruct the format string for you:

;; "~:<~:@{[~25s ~30s]~:^~:@_~}~:>"  ; the entire format string
;;  ~:<                        ~:>   ; creates a logical block
;;     ~:@{                  ~}      ; iterates through the vectors in the list
;;         [         ]               ; creates a pair of literal brackets
;;          ~25s ~30s                ; creates a pair of fixed-width columns
;;                    ~:^            ; breaks the iteration on the last pair
;;                       ~:@_        ; creates a mandatory newline



;; user=> (let [keys ["Attribute" "Value"]]
;;         (print-table keys (map (partial zipmap keys) attrs)))

;; =====================================================
;; Attribute       | Value                              
;; =====================================================
;; :db/cardinality | :db.cardinality/one                
;; :db/doc         | Documentation string for an entity.
;; :db/fulltext    | true                               
;; :db/ident       | :db/doc                            
;; :db/valueType   | :db.type/string                    
;; =====================================================



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmacro defun
  "defn with default positional arguments
   Example:

    (defun test-defun [a b [c 1] [d 1]]
      (+ a b c d))

    (test-defun 2 2) => 6
    (test-defun 2 2 2) => 7
    (test-defun 2 2 2 2) => 8)"
  [name args & body]
  (let [unpack-defaults
         (fn [args]
           (let [[undefaulted defaulted] (split-with (comp not vector?) args)
                  argcount (count args)]
             (loop [defaulted defaulted
                     argset {:argnames (into [] undefaulted)
                              :application (into [] (concat undefaulted (map second defaulted)))}
                     unpacked-args [argset]
                     position (count undefaulted)]
               (if (empty? defaulted)
                 unpacked-args```
                 (let [argname (ffirst defaulted)
                        new-argset {:argnames (conj (:argnames argset) argname)
                                     :application (assoc (:application argset) position argname)}]
                   (recur (rest defaulted) new-argset
                     (conj unpacked-args new-argset) (inc position)))))))
         unpacked-args (unpack-defaults args)]
    
    `(defn ~name
       (~(:argnames (last unpacked-args))
         ~@body)
       ~@(map #(list (:argnames %)
                 `(~name ~@(:application %)))
           (drop-last unpacked-args)))))



;;   (defun test-defun 
;;     [a b [c 1] [d 1]]
;;     (+ a b c d))
;;   (is (= 6 (test-defun 2 2)))
;;   (is (= 7 (test-defun 2 2 2)))
;;   (is (= 8 (test-defun 2 2 2 2)))



(defmacro wrap-fn [name args & body]
  `(let [old-fn# (var-get (var ~name))
         new-fn# (fn [& p#] 
                   (let [~args p#] 
                     (do ~@body)))
         wrapper# (fn [& params#]
                    (if (= ~(count args) (count params#))
                      (apply new-fn# params#)
                      (apply old-fn# params#)))] 
     (alter-var-root (var ~name) (constantly wrapper#))))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro with-temp-file [f-sym & body]
  `(let [prefix# (.toString (UUID/randomUUID))
         postfix# (.toString (UUID/randomUUID))
         ~f-sym (java.io.File/createTempFile prefix# postfix#)]
     (try
       (do ~@body)
       (finally
         (.delete ~f-sym)))))



;; (-> (with-out-str 
;;       (print-table (:members (reflect/reflect"foo"))))
;;   #(subs % 0 79))
(defmacro aif
  ([test-form then-form]
     `(let [~'it ~test-form]
        (if ~'it ~then-form)))
  ([test-form then-form else-form]
     `(let [~'it ~test-form]
        (if ~'it ~then-form ~else-form))))

(defmacro anil?
  ([test-form then-form]
     `(let [~'it ~test-form]
        (if-not (nil? ~'it) ~then-form)))
  ([test-form then-form else-form]
     `(let [~'it ~test-form]
        (if-not (nil? ~'it) ~then-form ~else-form))))

(defmacro awhen [test-form & body]
  `(aif ~test-form (do ~@body)))

(defmacro awhile [test-expr & body]
  `(while (let [~'it ~test-expr]
            (do ~@body)
            ~'it)))

(defmacro aand [& tests]
  (if (empty? tests)
    true
    (if (empty? (rest tests))
      (first tests)
      (let [first-test (first tests)]
        `(aif ~first-test
              (aand ~@(rest tests)))))))

(defmacro it-> [& [first-expr & rest-expr]]
  (if (empty? rest-expr)
    first-expr
    `(if-let [~'it ~first-expr]
       (it-> ~@rest-expr))))

(defmacro run-and-measure-timing [expr]
  `(let [start-time# (System/currentTimeMillis)
         response# ~expr
         end-time# (System/currentTimeMillis)]
     {:time-taken (- end-time# start-time#)
      :response response#
      :start-time start-time#
      :end-time end-time#}))

(defn lines-of-file [file-name]
 (line-seq
  (java.io.BufferedReader.
   (java.io.InputStreamReader.
    (java.io.FileInputStream. file-name)))))

(defmacro exception [& [param & more :as params]] 
  (if (class? param) 
    `(throw (new ~param (str ~@(interpose " " more)))) 
    `(throw (Exception. (str ~@(interpose " " params))))))
