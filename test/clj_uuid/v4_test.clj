(ns clj-uuid.v4-test
  "Custom UUIDs tests"
  (:require [clj-uuid.core  :as uuid]
            [clojure.test   :refer :all]))

(deftest check-v4-special-cases
  (testing "v4 special case correctness..."
    (is (= (uuid/v4  0  0)  #uuid "00000000-0000-4000-8000-000000000000"))
    (is (= (uuid/v4  0  1)  #uuid "00000000-0000-4000-8000-000000000001"))
    (is (= (uuid/v4  0 -1)  #uuid "00000000-0000-4000-bfff-ffffffffffff"))
    (is (= (uuid/v4 -1  0)  #uuid "ffffffff-ffff-4fff-8000-000000000000"))
    (is (= (uuid/v4 -1 -1)  #uuid "ffffffff-ffff-4fff-bfff-ffffffffffff"))))
