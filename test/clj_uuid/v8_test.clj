(ns clj-uuid.v8-test
  "Custom UUIDs tests"
  (:refer-clojure :exclude [uuid? max])
  (:require [clojure.test   :refer :all]
            [clj-uuid       :refer :all :exclude [> < =]]))

(deftest check-v8-special-cases
  (testing "v8 custom UUID"
    (is (= (v8  0  0)  #uuid "00000000-0000-8000-8000-000000000000"))
    (is (= (v8  0  1)  #uuid "00000000-0000-8000-8000-000000000001"))
    (is (= (v8  0 -1)  #uuid "00000000-0000-8000-bfff-ffffffffffff"))
    (is (= (v8 -1  0)  #uuid "ffffffff-ffff-8fff-8000-000000000000"))
    (is (= (v8 -1 -1)  #uuid "ffffffff-ffff-8fff-bfff-ffffffffffff"))))
