(ns clj-uuid.util-test
  (:require [clojure.test  :refer :all]
            [clj-uuid.util :refer :all]))

(deftest check-returning
  (testing "returns first value, executes side effects"
    (let [side-effect (atom nil)]
      (is (= 42 (returning 42
                  (reset! side-effect :done))))
      (is (= :done @side-effect))))

  (testing "returns first value with no side effects"
    (is (= :hello (returning :hello))))

  (testing "multiple side effects"
    (let [log (atom [])]
      (is (= "result"
             (returning "result"
               (swap! log conj :a)
               (swap! log conj :b))))
      (is (= [:a :b] @log)))))

(deftest check-java6?
  (testing "current JVM is not Java 6"
    ;; We're running on JDK 25, so java6? should be false
    (is (false? (java6?)))))

(deftest check-compile-if
  (testing "compile-if with truthy expression"
    (is (= :then (compile-if true :then :else))))

  (testing "compile-if with falsy expression"
    (is (= :else (compile-if false :then :else))))

  (testing "compile-if with exception-throwing expression"
    (is (= :else (compile-if (throw (Exception. "boom")) :then :else)))))

(deftest check-with-timing
  (testing "returns [result elapsed-ms] pair"
    (let [[result ms] (with-timing (+ 1 2))]
      (is (= 3 result))
      (is (number? ms))
      (is (>= ms 0.0))))

  (testing "measures non-trivial time"
    (let [[result ms] (with-timing (Thread/sleep 10) :done)]
      (is (= :done result))
      (is (>= ms 5.0) "sleep 10ms should measure at least 5ms"))))

(deftest check-with-temp-file
  (testing "creates and deletes temp file"
    (let [path (atom nil)]
      (with-temp-file f
        (reset! path (.getAbsolutePath f))
        (is (.exists f))
        (spit f "test content")
        (is (= "test content" (slurp f))))
      (is (not (.exists (java.io.File. @path)))
          "temp file should be deleted")))

  (testing "cleans up on exception"
    (let [path (atom nil)]
      (try
        (with-temp-file f
          (reset! path (.getAbsolutePath f))
          (throw (Exception. "test")))
        (catch Exception _))
      (is (not (.exists (java.io.File. @path)))
          "temp file should be deleted even on exception"))))

(defn ^:dynamic test-fn-for-wrap [x] (* x 2))

(deftest check-wrap-fn
  (testing "wrap-fn wraps a function"
    (let [original (var-get #'test-fn-for-wrap)]
      (wrap-fn test-fn-for-wrap [a] {:wrapped a})
      (is (= {:wrapped 5} (test-fn-for-wrap 5)))
      ;; Restore original
      (alter-var-root #'test-fn-for-wrap (constantly original)))))

(deftest check-lines-of-file
  (testing "reads lines from a temp file"
    (with-temp-file f
      (spit f "line1\nline2\nline3")
      (let [lines (doall (lines-of-file (.getAbsolutePath f)))]
        (is (= ["line1" "line2" "line3"] lines))))))
