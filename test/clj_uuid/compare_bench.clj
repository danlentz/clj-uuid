(ns clj-uuid.compare-bench
  "Apples-to-apples benchmarks: clj-uuid vs JUG vs uuid-creator vs JDK.

  Run:  lein test :only clj-uuid.compare-bench"
  (:require [clojure.test :refer :all]
            [clj-uuid     :as uuid])
  (:import [java.util UUID]
           [com.fasterxml.uuid Generators EthernetAddress]
           [com.fasterxml.uuid.impl
            TimeBasedGenerator
            TimeBasedReorderedGenerator
            TimeBasedEpochGenerator
            NameBasedGenerator
            RandomBasedGenerator]
           [com.github.f4b6a3.uuid UuidCreator]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Infrastructure
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private ^:const +n+ 500000)
(def ^:private ^:const +warmup+ 50000)

(defn- bench-ns
  "Run f for n iterations after warmup. Returns average ns/op."
  ^double [^long n f]
  (dotimes [_ +warmup+] (f))
  (let [start (System/nanoTime)]
    (dotimes [_ n] (f))
    (double (/ (- (System/nanoTime) start) n))))

(defn- bench-row
  "Benchmark f, return [label ns/op]."
  [label f]
  (let [ns-val (bench-ns +n+ f)]
    [label ns-val]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generators — pre-initialize outside the timing loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private ^TimeBasedGenerator          jug-v1  (Generators/timeBasedGenerator))
(def ^:private ^RandomBasedGenerator        jug-v4  (Generators/randomBasedGenerator))
(def ^:private ^NameBasedGenerator          jug-v5  (Generators/nameBasedGenerator))
(def ^:private ^TimeBasedReorderedGenerator jug-v6  (Generators/timeBasedReorderedGenerator))
(def ^:private ^TimeBasedEpochGenerator     jug-v7  (Generators/timeBasedEpochGenerator))

(def ^:private ^UUID jug-ns-dns
  (UUID/fromString "6ba7b810-9dad-11d1-80b4-00c04fd430c8"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Print helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- print-table-header []
  (println)
  (println (format "| %-20s | %14s | %14s | %14s | %14s |"
             "Operation" "clj-uuid" "JUG 5.2" "uuid-creator" "JDK"))
  (println (format "|%s|%s:|%s:|%s:|%s:|"
             (apply str (repeat 22 "-"))
             (apply str (repeat 15 "-"))
             (apply str (repeat 15 "-"))
             (apply str (repeat 15 "-"))
             (apply str (repeat 15 "-")))))

(defn- fmt-ns [v]
  (if v (format "%10.1f ns" v) (format "%14s" "--")))

(defn- print-row [label clj-ns jug-ns uc-ns jdk-ns]
  (println (format "| %-20s | %14s | %14s | %14s | %14s |"
             label (fmt-ns clj-ns) (fmt-ns jug-ns) (fmt-ns uc-ns) (fmt-ns jdk-ns))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Benchmarks
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest bench-comparison
  (testing "Apples-to-apples UUID library comparison"
    (print-table-header)

    ;; v1
    (let [[_ clj]  (bench-row "v1" #(uuid/v1))
          [_ jug]  (bench-row "v1" #(.generate jug-v1))
          [_ uc]   (bench-row "v1" #(UuidCreator/getTimeBased))]
      (print-row "v1 (time-based)" clj jug uc nil))

    ;; v4
    (let [[_ clj]  (bench-row "v4" #(uuid/v4))
          [_ jug]  (bench-row "v4" #(.generate jug-v4))
          [_ uc]   (bench-row "v4" #(UuidCreator/getRandomBased))
          [_ jdk]  (bench-row "v4" #(UUID/randomUUID))]
      (print-row "v4 (random)" clj jug uc jdk))

    ;; v5
    (let [[_ clj]  (bench-row "v5" #(uuid/v5 uuid/+namespace-dns+ "www.example.com"))
          [_ jug]  (bench-row "v5" #(.generate jug-v5 "www.example.com"))
          [_ uc]   (bench-row "v5" #(UuidCreator/getNameBasedSha1 jug-ns-dns "www.example.com"))]
      (print-row "v5 (SHA1)" clj jug uc nil))

    ;; v6
    (let [[_ clj]  (bench-row "v6" #(uuid/v6))
          [_ jug]  (bench-row "v6" #(.generate jug-v6))
          [_ uc]   (bench-row "v6" #(UuidCreator/getTimeOrdered))]
      (print-row "v6 (time-ordered)" clj jug uc nil))

    ;; v7
    (let [[_ clj]  (bench-row "v7" #(uuid/v7))
          [_ jug]  (bench-row "v7" #(.generate jug-v7))
          [_ uc]   (bench-row "v7" #(UuidCreator/getTimeOrderedEpoch))]
      (print-row "v7 (unix epoch)" clj jug uc nil))

    ;; v7nc
    (let [[_ clj]  (bench-row "v7nc" #(uuid/v7nc))
          [_ jug]  (bench-row "v7nc" #(.generate jug-v7))]
      (print-row "v7nc (fast epoch)" clj jug nil nil))

    ;; to-string
    (let [u (uuid/v4)]
      (let [[_ clj] (bench-row "str" #(uuid/to-string u))
            [_ jdk] (bench-row "str" #(.toString u))]
        (print-row "to-string" clj nil nil jdk)))

    ;; to-byte-array
    (let [u (uuid/v4)]
      (let [[_ clj] (bench-row "bytes" #(uuid/to-byte-array u))]
        (print-row "to-byte-array" clj nil nil nil)))

    (println)

    ;; Correctness checks
    (is (= 1 (.version (.generate jug-v1))))
    (is (= 4 (.version (.generate jug-v4))))
    (is (= 5 (.version (.generate jug-v5 "test"))))
    (is (= 6 (.version (.generate jug-v6))))
    (is (= 7 (.version (.generate jug-v7))))
    (is (= 1 (.version (UuidCreator/getTimeBased))))
    (is (= 4 (.version (UuidCreator/getRandomBased))))
    (is (= 6 (.version (UuidCreator/getTimeOrdered))))
    (is (= 7 (.version (UuidCreator/getTimeOrderedEpoch))))))
