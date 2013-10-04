(ns clj-uuid.constants
  (:use [clojure.core])
  (:use [clojure.pprint])
  (:use [clojure.repl :only [doc find-doc apropos]])
  (:import (java.net  URI URL))
  (:import (java.util UUID)))

(defonce +hex-chars+ ["0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "A" "B" "C" "D" "E" "F"])

(defonce +sign-mask+ 0x8000000000000000)

(defonce +ub63-mask+ 0x7fffffffffffffff)
(defonce +ub60-mask+ 0x0fffffffffffffff)
(defonce +ub56-mask+ 0x00ffffffffffffff)
(defonce +ub48-mask+ 0x0000ffffffffffff)
(defonce +ub40-mask+ 0x000000ffffffffff)
(defonce +ub32-mask+ 0x00000000ffffffff)
(defonce +ub24-mask+ 0x0000000000ffffff)
(defonce +ub16-mask+ 0x000000000000ffff)
(defonce +ub12-mask+ 0x0000000000000fff)
(defonce +ub8-mask+  0x00000000000000ff)
(defonce +ub4-mask+  0x000000000000000f)
(defonce +ub1-mask+  0x0000000000000001)

(defonce +namespace-dns+  #uuid"6ba7b810-9dad-11d1-80b4-00c04fd430c8")
(defonce +namespace-url+  #uuid"6ba7b811-9dad-11d1-80b4-00c04fd430c8")
(defonce +namespace-oid+  #uuid"6ba7b812-9dad-11d1-80b4-00c04fd430c8")
(defonce +namespace-x500+ #uuid"6ba7b814-9dad-11d1-80b4-00c04fd430c8")
(defonce +null+           #uuid"00000000-0000-0000-0000-000000000000")

(defonce &uuid-string     #"[0-9A-Fa-f]{8}(-[0-9A-Fa-f]{4}){3}-[0-9A-Fa-f]{12}")
(defonce &uuid-hex-string #"[0-9A-Fa-f]{32}")
(defonce &uuid-urn-string #"urn:uuid:[0-9A-Fa-f]{8}(-[0-9A-Fa-f]{4}){3}-[0-9A-Fa-f]{12}")
