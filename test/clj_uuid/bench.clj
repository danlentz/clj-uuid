(ns clj-uuid.bench
  "Benchmarks comparing clj-uuid (bitmop2) vs clj-uuid-old (bitmop)
  across all UUID versions and post-generation operations.

  Run:  lein test :only clj-uuid.bench"
  (:require [clojure.test :refer :all]
            [clj-uuid     :as uuid]
            [clj-uuid-old :as old]))


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

(defn- compare-perf
  "Benchmark old (f-old) vs new (f-new). Prints formatted line, returns map."
  [label f-old f-new]
  (let [ns-old (bench-ns +bench-n+ f-old)
        ns-new (bench-ns +bench-n+ f-new)
        ratio  (/ ns-old ns-new)]
    (println (format "  %-30s  old: %8.1f ns    new: %8.1f ns    speedup: %.2fx"
               label ns-old ns-new ratio))
    {:label label :old ns-old :new ns-new :ratio ratio}))

(defn- bench-single
  "Benchmark a single function. Prints formatted line, returns map."
  [label f]
  (let [ns-val (bench-ns +bench-n+ f)]
    (println (format "  %-30s  %8.1f ns" label ns-val))
    {:label label :ns ns-val}))

(defn- print-md-table
  "Print results as a markdown table."
  [header-label results]
  (println)
  (println (format "| %-24s | %14s | %14s | %7s |"
             header-label "clj-uuid-old (ns)" "clj-uuid (ns)" "Speedup"))
  (println (format "|%s|%s:|%s:|%s:|"
             (apply str (repeat 26 "-"))
             (apply str (repeat 18 "-"))
             (apply str (repeat 16 "-"))
             (apply str (repeat 9 "-"))))
  (doseq [{:keys [label old new ratio]} results]
    (println (format "| %-24s | %14.1f   | %14.1f   | %5.2fx  |"
               label old new ratio))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 1. UUID Generation (Pure Construction)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest bench-uuid-generation
  (testing "UUID Generation (Pure Construction)"
    (println)
    (println "=== 1. UUID Generation (Pure Construction) ===")
    (println)
    (let [;; v3/v5 inputs
          ns-uuid   uuid/+namespace-dns+
          ns-old    old/+namespace-dns+
          test-name "www.example.com"
          results
          [(compare-perf "v1 (time-based)"
             #(old/v1) #(uuid/v1))
           (compare-perf "v3 (MD5, namespace)"
             #(old/v3 ns-old test-name) #(uuid/v3 ns-uuid test-name))
           (compare-perf "v4 (random)"
             #(old/v4) #(uuid/v4))
           (compare-perf "v5 (SHA1, namespace)"
             #(old/v5 ns-old test-name) #(uuid/v5 ns-uuid test-name))
           (compare-perf "v6 (time-based, sorted)"
             #(old/v6) #(uuid/v6))
           (compare-perf "v7 (unix time, crypto)"
             #(old/v7) #(uuid/v7))
           (compare-perf "v8 (custom)"
             #(old/v8 0x123456789ABCDEF -1)
             #(uuid/v8 0x123456789ABCDEF -1))]]
      (println)
      (print-md-table "UUID Version" results)

      ;; Verify correctness: v3/v5 should produce identical UUIDs
      (is (= (uuid/v3 ns-uuid test-name) (old/v3 ns-old test-name))
        "v3 produces identical UUIDs")
      (is (= (uuid/v5 ns-uuid test-name) (old/v5 ns-old test-name))
        "v5 produces identical UUIDs"))))


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
          [(compare-perf "to-byte-array"
             #(old/to-byte-array test-uuid) #(uuid/to-byte-array test-uuid))
           (compare-perf "to-hex-string"
             #(old/to-hex-string test-uuid) #(uuid/to-hex-string test-uuid))
           (compare-perf "to-string"
             #(old/to-string test-uuid) #(uuid/to-string test-uuid))
           (compare-perf "to-urn-string"
             #(old/to-urn-string test-uuid) #(uuid/to-urn-string test-uuid))
           (compare-perf "get-version"
             #(old/get-version test-uuid) #(uuid/get-version test-uuid))
           (compare-perf "get-node-id"
             #(old/get-node-id test-uuid) #(uuid/get-node-id test-uuid))]]
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
          ns-old    old/+namespace-dns+
          test-name "www.example.com"
          results
          [(compare-perf "v1 + to-byte-array"
             #(old/to-byte-array (old/v1))
             #(uuid/to-byte-array (uuid/v1)))
           (compare-perf "v3 + to-byte-array"
             #(old/to-byte-array (old/v3 ns-old test-name))
             #(uuid/to-byte-array (uuid/v3 ns-uuid test-name)))
           (compare-perf "v3 + to-hex-string"
             #(old/to-hex-string (old/v3 ns-old test-name))
             #(uuid/to-hex-string (uuid/v3 ns-uuid test-name)))
           (compare-perf "v4 + to-byte-array"
             #(old/to-byte-array (old/v4))
             #(uuid/to-byte-array (uuid/v4)))
           (compare-perf "v4 + to-hex-string"
             #(old/to-hex-string (old/v4))
             #(uuid/to-hex-string (uuid/v4)))
           (compare-perf "v5 + to-byte-array"
             #(old/to-byte-array (old/v5 ns-old test-name))
             #(uuid/to-byte-array (uuid/v5 ns-uuid test-name)))
           (compare-perf "v5 + to-hex-string"
             #(old/to-hex-string (old/v5 ns-old test-name))
             #(uuid/to-hex-string (uuid/v5 ns-uuid test-name)))
           (compare-perf "v7 + to-byte-array"
             #(old/to-byte-array (old/v7))
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
          ns-old    old/+namespace-dns+
          test-name "www.example.com"
          versions  [["v1 (time-based)"         #(old/v1)                      #(uuid/v1)]
                     ["v3 (MD5, namespace)"      #(old/v3 ns-old test-name)     #(uuid/v3 ns-uuid test-name)]
                     ["v4 (random)"              #(old/v4)                      #(uuid/v4)]
                     ["v5 (SHA1, namespace)"      #(old/v5 ns-old test-name)     #(uuid/v5 ns-uuid test-name)]
                     ["v6 (time-based, sorted)"  #(old/v6)                      #(uuid/v6)]
                     ["v7 (unix time, crypto)"   #(old/v7)                      #(uuid/v7)]
                     ["v8 (custom)"              #(old/v8 0x123 0x456)          #(uuid/v8 0x123 0x456)]]]
      (println (format "  %-28s  %18s  %18s" "UUID Version" "clj-uuid-old (ops/s)" "clj-uuid (ops/s)"))
      (println (format "  %-28s  %18s  %18s" (apply str (repeat 28 "-")) (apply str (repeat 18 "-")) (apply str (repeat 18 "-"))))
      (doseq [[label f-old f-new] versions]
        (let [ns-old (bench-ns +bench-n+ f-old)
              ns-new (bench-ns +bench-n+ f-new)
              ops-old (long (/ 1e9 ns-old))
              ops-new (long (/ 1e9 ns-new))]
          (println (format "  %-28s  %,18d  %,18d" label ops-old ops-new)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 5. v3/v5 Detailed Breakdown
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest bench-v3-v5-detailed
  (testing "v3/v5 Detailed Breakdown"
    (println)
    (println "=== 5. v3/v5 Detailed Breakdown ===")
    (println)
    (let [ns-uuid   uuid/+namespace-dns+
          ns-old    old/+namespace-dns+
          test-name "www.example.com"]

      (println "--- v3 (MD5) ---")
      (compare-perf "v3 generation"
        #(old/v3 ns-old test-name) #(uuid/v3 ns-uuid test-name))
      (let [v3-old (old/v3 ns-old test-name)
            v3-new (uuid/v3 ns-uuid test-name)]
        (compare-perf "v3 + to-byte-array"
          #(old/to-byte-array v3-old) #(uuid/to-byte-array v3-new))
        (compare-perf "v3 + to-hex-string"
          #(old/to-hex-string v3-old) #(uuid/to-hex-string v3-new))
        (compare-perf "v3 + to-string"
          #(old/to-string v3-old) #(uuid/to-string v3-new)))

      (println)
      (println "--- v5 (SHA1) ---")
      (compare-perf "v5 generation"
        #(old/v5 ns-old test-name) #(uuid/v5 ns-uuid test-name))
      (let [v5-old (old/v5 ns-old test-name)
            v5-new (uuid/v5 ns-uuid test-name)]
        (compare-perf "v5 + to-byte-array"
          #(old/to-byte-array v5-old) #(uuid/to-byte-array v5-new))
        (compare-perf "v5 + to-hex-string"
          #(old/to-hex-string v5-old) #(uuid/to-hex-string v5-new))
        (compare-perf "v5 + to-string"
          #(old/to-string v5-old) #(uuid/to-string v5-new)))

      ;; Correctness
      (is (= (uuid/v3 ns-uuid test-name) (old/v3 ns-old test-name)))
      (is (= (uuid/v5 ns-uuid test-name) (old/v5 ns-old test-name)))
      (is (= 3 (uuid/get-version (uuid/v3 ns-uuid test-name))))
      (is (= 5 (uuid/get-version (uuid/v5 ns-uuid test-name)))))))
