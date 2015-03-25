(ns clj-uuid.constants)


(def +md5+  "MD5")
(def +sha1+ "SHA1")


(def uuid-regex  #"[0-9A-Fa-f]{8}(-[0-9A-Fa-f]{4}){3}-[0-9A-Fa-f]{12}")
(def hex-regex   #"[0-9A-Fa-f]{32}")
(def urn-regex   #"urn:uuid:[0-9A-Fa-f]{8}(-[0-9A-Fa-f]{4}){3}-[0-9A-Fa-f]{12}")

(def +hex-chars+ [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \A \B \C \D \E \F])

(def ^:const +ub63-mask+ 0x7fffffffffffffff)
(def ^:const +ub60-mask+ 0x0fffffffffffffff)
(def ^:const +ub56-mask+ 0x00ffffffffffffff)
(def ^:const +ub48-mask+ 0x0000ffffffffffff)
(def ^:const +ub40-mask+ 0x000000ffffffffff)
(def ^:const +ub32-mask+ 0x00000000ffffffff)
(def ^:const +ub24-mask+ 0x0000000000ffffff)
(def ^:const +ub16-mask+ 0x000000000000ffff)
(def ^:const +ub12-mask+ 0x0000000000000fff)
(def ^:const +ub8-mask+  0x00000000000000ff)
(def ^:const +ub4-mask+  0x000000000000000f)
(def ^:const +ub1-mask+  0x0000000000000001)
