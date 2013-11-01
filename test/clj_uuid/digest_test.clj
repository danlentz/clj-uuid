(ns clj-uuid.digest-test
  (:require
    [clojure.test    :refer :all]
    [clj-uuid.bitmop :refer :all]
    [clj-uuid.digest :refer :all]))


(deftest check-md5-digest
  (testing "md5..."    
    (is (=
          (map ub8 (md5 "xyz"))
          (209 111 179 111 9 17 248 120 153 140 19 97 145 175 112 94)
          [-47 111 -77 111 9 17 -8 120 -103 -116 19 97 -111 -81 112 94]
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


(= (sha1 "x")   [ 17 -10 -83 -114 -59 42 41 -124 -85 -86 -3 124 59 81 101 3 120 92 32 114])
(= (sha1 "xyz") [102 -78 116 23 -45 126 2 76 70 82 108 47 109 53 -118 117 79 -59 82 -13])
(= (md5  "x")   [-99 -44 -28 97 38 -116 -128 52 -11 -56 86 78 21 92 103 -90])
(= (md5  "xyz") [-47 111 -77 111 9 17 -8 120 -103 -116 19 97 -111 -81 112 94])


(digest/digest-uuid digest/md5 +namespace-x500+)
[224 243 83 182 109 150 143 201 22 142 59 181 235 163 109 74]
(digest/digest-uuid digest/md5 +namespace-x500+ "")
[224 243 83 182 109 150 143 201 22 142 59 181 235 163 109 74]

(digest/digest-uuid digest/md5 +namespace-x500+ "localpart")
[206 235 160 83 186 123 69 138 234 73 194 222 81 114 102 101]
[206 235 160 83 186 123 69 138 234 73 194 222 81 114 102 101]

(digest/digest-uuid digest/md5 +namespace-x500+ "local" "part")
[206 235 160 83 186 123 69 138 234 73 194 222 81 114 102 101]
[206 235 160 83 186 123 69 138 234 73 194 222 81 114 102 101]

(digest/digest-uuid digest/md5 +namespace-x500+ "l" "o" "cal" "p" "a" "r" "t")
[206 235 160 83 186 123 69 138 234 73 194 222 81 114 102 101]
[206 235 160 83 186 123 69 138 234 73 194 222 81 114 102 101]

(digest/digest-uuid digest/sha1 +namespace-x500+)
[116 137 33 116 46 168 229 130 235 167 122 54 250 82 58 47]
(digest/digest-uuid digest/sha1 +namespace-x500+ "")
[116 137 33 116 46 168 229 130 235 167 122 54 250 82 58 47]

(digest/digest-uuid digest/sha1 +namespace-x500+ "localpart")
[95 64 157 36 8 165 179 255 91 252 46 81 229 3 129 157]
[95 64 157 36 8 165 179 255 91 252 46 81 229 3 129 157]

(digest/digest-uuid digest/sha1 +namespace-x500+ "local" "part")
[95 64 157 36 8 165 179 255 91 252 46 81 229 3 129 157]
[95 64 157 36 8 165 179 255 91 252 46 81 229 3 129 157]

(digest/digest-uuid digest/sha1 +namespace-x500+ "l" "o" "cal" "p" "a" "r" "t")
[95 64 157 36 8 165 179 255 91 252 46 81 229 3 129 157]
[95 64 157 36 8 165 179 255 91 252 46 81 229 3 129 157]

