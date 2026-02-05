(ns clj-uuid.bench
  "Benchmarks for clj-uuid UUID generation and post-generation operations.

  Run:  lein test :only clj-uuid.bench"
  (:require [clojure.test :refer :all]
            [clj-uuid     :as uuid]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Benchmark Infrastructure
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private ^:const +bench-n+ 500000)
(def ^:private ^:const +warmup+   50000)

(defn- bench-ns
  "Run `f` for `n` iterations after warmup. Returns average ns/op."
  ^double [^long n f]
  (dotimes [_ +warmup+] (f))
  (let [start (System/nanoTime)]
    (dotimes [_ n] (f))
    (double (/ (- (System/nanoTime) start) n))))

(defn- bench-print
  "Benchmark `f` and print a formatted result line. Returns map with label and ns."
  [label f]
  (let [ns-val (bench-ns +bench-n+ f)]
    (println (format "  %-34s  %8.1f ns" label ns-val))
    {:label label :ns ns-val}))

(defn- print-md-table
  "Print results as a markdown table."
  [header-label results]
  (println)
  (println (format "| %-34s | %14s |" header-label "ns/op"))
  (println (format "|%s|%s:|" (apply str (repeat 36 "-")) (apply str (repeat 16 "-"))))
  (doseq [{:keys [label ns]} results]
    (println (format "| %-34s | %14.1f   |" label ns))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 1. UUID Generation (Pure Construction)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest bench-uuid-generation
  (testing "UUID Generation (Pure Construction)"
    (println)
    (println "=== 1. UUID Generation (Pure Construction) ===")
    (println)
    (let [ns-uuid   uuid/+namespace-dns+
          test-name "www.example.com"
          results
          [(bench-print "v1 (time-based)"            #(uuid/v1))
           (bench-print "v3 (MD5, namespace)"        #(uuid/v3 ns-uuid test-name))
           (bench-print "v4 (random)"                #(uuid/v4))
           (bench-print "v5 (SHA1, namespace)"       #(uuid/v5 ns-uuid test-name))
           (bench-print "v6 (time-based, sorted)"    #(uuid/v6))
           (bench-print "v7 (unix time, crypto)"     #(uuid/v7))
           (bench-print "v7nc (unix time, fast)"     #(uuid/v7nc))
           (bench-print "v8 (custom)"                #(uuid/v8 0x123456789ABCDEF -1))]]
      (println)
      (print-md-table "UUID Version" results)

      ;; Verify correctness
      (is (= 1 (uuid/get-version (uuid/v1))))
      (is (= 3 (uuid/get-version (uuid/v3 ns-uuid test-name))))
      (is (= 4 (uuid/get-version (uuid/v4))))
      (is (= 5 (uuid/get-version (uuid/v5 ns-uuid test-name))))
      (is (= 6 (uuid/get-version (uuid/v6))))
      (is (= 7 (uuid/get-version (uuid/v7))))
      (is (= 7 (uuid/get-version (uuid/v7nc))))
      (is (= 8 (uuid/get-version (uuid/v8 0x123456789ABCDEF -1))))
      ;; v3/v5 are deterministic
      (is (= (uuid/v3 ns-uuid test-name) (uuid/v3 ns-uuid test-name)))
      (is (= (uuid/v5 ns-uuid test-name) (uuid/v5 ns-uuid test-name))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 2. Post-Generation Operations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest bench-post-generation
  (testing "Post-Generation Operations"
    (println)
    (println "=== 2. Post-Generation Operations ===")
    (println)
    (let [test-uuid (java.util.UUID/randomUUID)
          results
          [(bench-print "to-byte-array"  #(uuid/to-byte-array test-uuid))
           (bench-print "to-hex-string"  #(uuid/to-hex-string test-uuid))
           (bench-print "to-string"      #(uuid/to-string test-uuid))
           (bench-print "to-urn-string"  #(uuid/to-urn-string test-uuid))
           (bench-print "get-version"    #(uuid/get-version test-uuid))
           (bench-print "get-node-id"    #(uuid/get-node-id test-uuid))]]
      (println)
      (print-md-table "Operation" results))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 3. Combined: Generate + Serialize
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest bench-combined
  (testing "Combined: Generate + Serialize"
    (println)
    (println "=== 3. Combined: Generate + Serialize ===")
    (println)
    (let [ns-uuid   uuid/+namespace-dns+
          test-name "www.example.com"
          results
          [(bench-print "v1 + to-byte-array"
             #(uuid/to-byte-array (uuid/v1)))
           (bench-print "v3 + to-byte-array"
             #(uuid/to-byte-array (uuid/v3 ns-uuid test-name)))
           (bench-print "v3 + to-hex-string"
             #(uuid/to-hex-string (uuid/v3 ns-uuid test-name)))
           (bench-print "v4 + to-byte-array"
             #(uuid/to-byte-array (uuid/v4)))
           (bench-print "v4 + to-hex-string"
             #(uuid/to-hex-string (uuid/v4)))
           (bench-print "v5 + to-byte-array"
             #(uuid/to-byte-array (uuid/v5 ns-uuid test-name)))
           (bench-print "v5 + to-hex-string"
             #(uuid/to-hex-string (uuid/v5 ns-uuid test-name)))
           (bench-print "v7 + to-byte-array"
             #(uuid/to-byte-array (uuid/v7)))]]
      (println)
      (print-md-table "Operation" results))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 4. Absolute Throughput
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest bench-throughput
  (testing "Absolute Throughput (ops/sec)"
    (println)
    (println "=== 4. Absolute Throughput ===")
    (println)
    (let [ns-uuid   uuid/+namespace-dns+
          test-name "www.example.com"
          versions  [["v1 (time-based)"         #(uuid/v1)]
                     ["v3 (MD5, namespace)"      #(uuid/v3 ns-uuid test-name)]
                     ["v4 (random)"              #(uuid/v4)]
                     ["v5 (SHA1, namespace)"     #(uuid/v5 ns-uuid test-name)]
                     ["v6 (time-based, sorted)"  #(uuid/v6)]
                     ["v7 (unix time, crypto)"   #(uuid/v7)]
                     ["v8 (custom)"              #(uuid/v8 0x123 0x456)]]]
      (println (format "  %-34s  %18s" "UUID Version" "ops/s"))
      (println (format "  %-34s  %18s" (apply str (repeat 34 "-")) (apply str (repeat 18 "-"))))
      (doseq [[label f] versions]
        (let [ns-val (bench-ns +bench-n+ f)
              ops    (long (/ 1e9 ns-val))]
          (println (format "  %-34s  %,18d" label ops)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 5. v3/v5 Detailed Breakdown
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest bench-v3-v5-detailed
  (testing "v3/v5 Detailed Breakdown"
    (println)
    (println "=== 5. v3/v5 Detailed Breakdown ===")
    (println)
    (let [ns-uuid   uuid/+namespace-dns+
          test-name "www.example.com"]

      (println "--- v3 (MD5) ---")
      (bench-print "v3 generation"
        #(uuid/v3 ns-uuid test-name))
      (let [v3-uuid (uuid/v3 ns-uuid test-name)]
        (bench-print "v3 + to-byte-array"
          #(uuid/to-byte-array v3-uuid))
        (bench-print "v3 + to-hex-string"
          #(uuid/to-hex-string v3-uuid))
        (bench-print "v3 + to-string"
          #(uuid/to-string v3-uuid)))

      (println)
      (println "--- v5 (SHA1) ---")
      (bench-print "v5 generation"
        #(uuid/v5 ns-uuid test-name))
      (let [v5-uuid (uuid/v5 ns-uuid test-name)]
        (bench-print "v5 + to-byte-array"
          #(uuid/to-byte-array v5-uuid))
        (bench-print "v5 + to-hex-string"
          #(uuid/to-hex-string v5-uuid))
        (bench-print "v5 + to-string"
          #(uuid/to-string v5-uuid)))

      ;; Correctness
      (is (= 3 (uuid/get-version (uuid/v3 ns-uuid test-name))))
      (is (= 5 (uuid/get-version (uuid/v5 ns-uuid test-name)))))))
