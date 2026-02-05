(ns clj-uuid.clock-test
  (:require [clojure.test   :refer :all]
            [clojure.set]
            [clj-uuid.clock :refer :all])
  (:import [clj_uuid.clock State]))

(deftest check-single-threaded
  (let [iterations 1000000
        groups     10
        check      #(mapv (fn [_] (%)) (range iterations))]
    (testing "monotonic-time..."
      (dotimes [_ groups]
        (let [result   (check monotonic-time)]
          (is (= (count result) (count (set result)))))))
    (testing "monotonic-unix-time-and-random-counter..."
      (dotimes [_ groups]
        (let [result   (check monotonic-unix-time-and-random-counter)
              pairs    (mapv #(vector (.millis ^State %) (.seqid ^State %)) result)]
          (is (= (count pairs) (count (set pairs)))))))))

(deftest check-multi-threaded-monotonic-time
  (doseq [concur (range 0 9)]
    (let [extent    1000000
          agents    (mapv agent (repeat concur nil))
          working   (mapv #(send-off %
                            (fn [state]
                              (repeatedly extent monotonic-time)))
                      agents)
          _         (apply await working)
          answers   (mapv deref working)]
      (testing (str "concurrent timestamp uniqueness (" concur " threads)...")
        (is (= (* concur extent)
               (count (apply clojure.set/union (map set answers))))))
      (testing (str "concurrent monotonic increasing (" concur " threads)...")
        (is (every? identity
                    (map #(apply < %) answers)))))))

(deftest check-multi-threaded-monotonic-unix-time-and-random-counter
  (doseq [concur (range 0 9)]
    (let [extent  1000000
          agents  (mapv agent (repeat concur nil))
          working (mapv #(send-off %
                           (fn [state]
                             (repeatedly extent
                                         monotonic-unix-time-and-random-counter)))
                         agents)
          _       (apply await working)
          answers (mapv deref working)]
      (testing (str "concurrent timestamp uniqueness (" concur " threads)...")
        (is (=
              (* concur extent)
              (count (apply clojure.set/union
                       (map #(set (map (fn [^State s] [(.millis s) (.seqid s)]) %))
                            answers))))))
      (testing (str "concurrent monotonic increasing (" concur " threads)...")
        (doseq [answer answers]
          (let [^State first-state (first answer)]
            (loop [time    (.millis first-state)
                   counter (.seqid first-state)
                   more    (rest answer)]
              (when-let [^State next-state (first more)]
                (let [next-time    (.millis next-state)
                      next-counter (.seqid next-state)]
                  (cond
                    (< next-time time)
                    (is false "time must be increasing")

                    (and (= next-time time) (<= next-counter counter))
                    (is false "counter must be increasing")

                    :else
                    (recur next-time next-counter (rest more))))))))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Time Conversion Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest check-posix-time-zero-arity
  (testing "posix-time with no args returns a number"
    (is (number? (posix-time)))))

(deftest check-posix-time-with-arg
  (testing "posix-time converts gregorian to POSIX"
    (let [greg 131059232331511828
          pt   (posix-time greg)]
      (is (number? pt))
      (is (= (- (quot 131059232331511828 10000) 12219292800000) pt)))))

(deftest check-universal-time-zero-arity
  (testing "universal-time returns a number"
    (is (number? (universal-time)))))

(deftest check-universal-time-with-arg
  (testing "universal-time converts gregorian to universal time"
    (let [greg 131059232331511828
          ut   (universal-time greg)
          pt   (posix-time greg)]
      (is (= (+ pt 2208988800) ut)))))

(deftest check-time-conversion-consistency
  (testing "posix-time and universal-time differ by epoch offset"
    (let [greg (monotonic-time)]
      (is (= 2208988800 (- (universal-time greg) (posix-time greg)))))))
