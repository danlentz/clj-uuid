(ns clj-uuid.constants
  (:use [clojure.core])
  (:use [clojure.pprint])
  (:use [clojure.repl :only [doc find-doc apropos]])
  (:import (java.net  URI URL))
  (:import (java.util UUID)))

(defonce hex-chars ["0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "A" "B" "C" "D" "E" "F"])

(defonce +ub56-mask+ 0x00ffffffffffffff)
(defonce +ub48-mask+ 0x0000ffffffffffff)
(defonce +ub40-mask+ 0x000000ffffffffff)
(defonce +ub32-mask+ 0x00000000ffffffff)
(defonce +ub24-mask+ 0x0000000000ffffff)
(defonce +ub16-mask+ 0x000000000000ffff)
(defonce +ub8-mask+  0x00000000000000ff)
(defonce +ub4-mask+  0x000000000000000f)
      
(defonce +namespace-dns+  (UUID/fromString "6ba7b810-9dad-11d1-80b4-00c04fd430c8"))
(defonce +namespace-url+  (UUID/fromString "6ba7b811-9dad-11d1-80b4-00c04fd430c8"))
(defonce +namespace-oid+  (UUID/fromString "6ba7b812-9dad-11d1-80b4-00c04fd430c8"))
(defonce +namespace-x500+ (UUID/fromString "6ba7b814-9dad-11d1-80b4-00c04fd430c8"))
(defonce +null+           (UUID/fromString "00000000-0000-0000-0000-000000000000"))


