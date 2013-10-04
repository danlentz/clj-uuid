(ns clj-uuid.digest-test
  (:require
    [clojure.test    :refer :all]
    [clj-uuid.bitmop :refer :all]
    [clj-uuid.digest :refer :all]))


(deftest check-md5-digest
  (testing "md5..."    
    (is (=
          (md5 "xyz")
          (md5 ["xyz"])
          [209 111 179 111 9 17 248 120 153 140 19 97 145 175 112 94]))
    (is (=
          (md5 "xyzabc")
          (md5 ["xyzabc"])
          (md5 ["xyz" "abc"])
          [71 45 201 14 212 57 209 24 173 58 32 93 218 150 234 240]))))

(deftest check-sha1-digest
  (testing "sha1..."    
    (is (=
          (sha1 "xyz")
          (sha1 ["xyz"])
          [102 178 116 23 211 126 2 76 70 82 108 47 109 53 138 117 79 197 82 243]))
    (is (=
          (sha1 "abcdef")
          (sha1 ["abcdef"])
          (sha1 ["abc" "def"])
          [31 138 193 15 35 197 181 188 17 103 189 168 75 131 62 92 5 122 119 210]))))

