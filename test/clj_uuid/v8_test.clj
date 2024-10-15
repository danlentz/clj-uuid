(ns clj-uuid.v8-test
  "Custom UUIDs tests"
  (:require [clj-uuid.core  :as uuid]
            [clojure.test   :refer :all]))

(deftest check-v8-special-cases
  (testing "v8 custom UUID"
    (is (= (uuid/v8  0  0)  #uuid "00000000-0000-8000-8000-000000000000"))
    (is (= (uuid/v8  0  1)  #uuid "00000000-0000-8000-8000-000000000001"))
    (is (= (uuid/v8  0 -1)  #uuid "00000000-0000-8000-bfff-ffffffffffff"))
    (is (= (uuid/v8 -1  0)  #uuid "ffffffff-ffff-8fff-8000-000000000000"))
    (is (= (uuid/v8 -1 -1)  #uuid "ffffffff-ffff-8fff-bfff-ffffffffffff"))))
