(ns danlentz.clj-uuid.clock-test
  (:require [clojure.test   :refer :all]
            [clojure.set]
            [danlentz.clj-uuid.clock :refer :all]))

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
        (let [result   (check monotonic-unix-time-and-random-counter)]
          (is (= (count result) (count (set result)))))))))

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
              (count (apply clojure.set/union (map set answers))))))
      (testing (str "concurrent monotonic increasing (" concur " threads)...")
        (doseq [answer answers]
          (let [[time counter] (first answer)]
            (loop [time    time
                   counter counter
                   more    (rest answer)]
              (when-let [[next-time next-counter] (first more)]
                (cond
                  (< next-time time)
                  (is false "time must be increasing")

                  (and (= next-time time) (<= next-counter counter))
                  (is false "counter must be increasing")

                  :else
                  (recur next-time next-counter (rest more)))))))))))
