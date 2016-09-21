(ns clj-uuid.v4-test
  (:refer-clojure :exclude [uuid?])
  (:require [clojure.test   :refer :all]
            [clj-uuid       :refer :all]))


(deftest check-v4-special-cases
  (testing "v4 special case correctness..."
    (is (= (v4  0  0)  #uuid "00000000-0000-4000-8000-000000000000"))
    (is (= (v4  0  1)  #uuid "00000000-0000-4000-8000-000000000001"))
    (is (= (v4  0 -1)  #uuid "00000000-0000-4000-bfff-ffffffffffff"))
    (is (= (v4 -1  0)  #uuid "ffffffff-ffff-4fff-8000-000000000000"))
    (is (= (v4 -1 -1)  #uuid "ffffffff-ffff-4fff-bfff-ffffffffffff"))))
