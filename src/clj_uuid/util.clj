(ns clj-uuid.util
  (:import (java.util UUID)))




(defn indexed
  "Returns a lazy sequence of [index, item] pairs, where items come
  from 's' and indexes count up from zero.
  (indexed '(a b c d))  =>  ([0 a] [1 b] [2 c] [3 d])"
  [s]
  ;; (map vector (iterate inc 0) s))
  (map #(clojure.lang.MapEntry. %1 %2) (range) s))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PROG1 but with more idiomatic clojure name 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro returning
  "Compute a return value, then execute other forms for side effects.
  Like prog1 in common lisp, or a (do) that returns the first form."
  [value & forms]
  `(let [value# ~value]
     ~@forms
     value#))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Conditional Compilation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn java6? []
  (neg? (compare (System/getProperty "java.version") "1.7")))

(defmacro compile-if
  "Evaluate `exp` and if it returns logical true and doesn't error, expand to
  `then` otherwise expand to `else`.  
  credit: <clojure/src/clj/clojure/core/reducers.clj#L24>

  (compile-if (Class/forName \"java.util.concurrent.ForkJoinTask\")
    (do-cool-stuff-with-fork-join)
    (fall-back-to-executor-services))"
  [exp then else]
  (if (try (eval exp)
           (catch Throwable _ false))
    `(do ~then)
    `(do ~else)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Timing and Performance Metric
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro with-timing
  "Same as clojure.core/time but returns a vector of a the result of
   the code and the milliseconds rather than printing a string. Runs
   the code in an implicit do."
  [& body]
  `(let [start# (System/nanoTime)  ret# ~(cons 'do body)]
     [ret# (/ (double (- (System/nanoTime) start#)) 1000000.0)]))


(defmacro run-and-measure-timing [expr]
  `(let [start-time# (System/currentTimeMillis)
         response# ~expr
         end-time# (System/currentTimeMillis)]
     {:time-taken (- end-time# start-time#)
      :response response#
      :start-time start-time#
      :end-time end-time#}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Debugging
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; IO
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro with-temp-file [f-sym & body]
  `(let [prefix#  (.toString (UUID/randomUUID))
         postfix# (.toString (UUID/randomUUID))
         ~f-sym   (java.io.File/createTempFile prefix# postfix#)]
     (try
       (do ~@body)
       (finally
         (.delete ~f-sym)))))

(defn lines-of-file [^String file-name]
 (line-seq
  (java.io.BufferedReader.
   (java.io.InputStreamReader.
    (java.io.FileInputStream. file-name)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Condition Handling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro exception [& [param & more :as params]] 
  (if (class? param) 
    `(throw (new ~param (str ~@(interpose " " more)))) 
    `(throw (Exception. (str ~@(interpose " " params))))))
