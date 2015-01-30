(ns clj-uuid.clock
  (:require [clj-uuid.constants :refer :all]
            [clj-uuid.bitmop :refer :all])
  (:import [java.net InetAddress NetworkInterface]
           [java.sql Timestamp]
           [java.security MessageDigest]
           [java.util Properties Date]
           [java.nio.charset StandardCharsets]))

(defn all-local-addresses []
  (let [^InetAddress local-host (InetAddress/getLocalHost)
        host-name (.getCanonicalHostName local-host)
        base-addresses #{(str local-host) host-name}
        network-interfaces (reduce (fn [acc ^NetworkInterface ni]
                                     (apply conj acc (map str (enumeration-seq (.getInetAddresses ni)))))
                                   base-addresses
                                   (enumeration-seq (NetworkInterface/getNetworkInterfaces)))]
    (reduce conj network-interfaces (map str (InetAddress/getAllByName host-name)))))

(def make-node-id
  (memoize
   (fn []
     (let [addresses (all-local-addresses)
           ^MessageDigest digest (MessageDigest/getInstance "MD5")
           ^Properties props (System/getProperties)
           to-digest (reduce (fn [acc key]
                               (conj acc (.getProperty props key)))
                             addresses
                             ["java.vendor" "java.vendor.url" "java.version" "os.arch" "os.name" "os.version"])]
       (doseq [^String d to-digest]
         (.update digest (.getBytes d StandardCharsets/UTF_8)))
       (->> (.digest digest)
            (map ub8)
            ubvec
            (#(subvec % 0 6)))))))

(def +node-id+          (make-node-id))
(def +tick-resolution+  9999)
(def +startup-nanotime+ (System/nanoTime))

(def stamps-this-tick (atom 0))
(def last-time       (atom 0))
(def clock-seq       (atom (+ (rand-int 9999) 1)))

(defn get-internal-real-time []
  (/ (- (System/nanoTime) +startup-nanotime+) 1000))

(defn get-universal-time []
  (quot (System/currentTimeMillis) 1000))

(defn timestamp
  ([]
   (Timestamp. (.getTime (Date.))))
  ([epoch-millis]
   (Timestamp. epoch-millis)))

(defn now []
  (timestamp))

(defn timestamp-since-epoch-millis [ts]
  (.getTime ts))

(defn get-epoch-time []
  (timestamp-since-epoch-millis (now)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; epoch vs universal time: epoch stamp is 1000x higher resolution (milliseconds)
;; universal is representation in seconds. Either way we pad to achieve the rfc4122
;; compat and support atomic incremental uuids-this-second subcounter on the low
;; order bits, guarantee that never collides regardless of clock precision.
;;
;; ..............
;; 113914335216380000  = (+ (* (get-epoch-time)     10000)    100103040000000000)
;; ...........
;; 113914335210000000  = (+ (* (get-universal-time) 10000000) 100103040000000000))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-timestamp []
  (letfn [(ts []
            (+ (* (get-epoch-time)     10000)    100103040000000000))]
    (loop [time-now (ts)]
      (if (not (= @last-time time-now))
        (do
          (swap! stamps-this-tick (constantly 0))
          (swap! last-time (constantly time-now))
          time-now)
        (if (< @stamps-this-tick +tick-resolution+)
          (+ time-now (swap! stamps-this-tick inc))
          (recur (ts)))))))



;; Verify 60 bit identity/equality
(assert (let [x (get-epoch-time)]
          (= x (ldb (mask 60 0) x))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Timing and Performance Metric
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro with-timing-and-result
  "Same as clojure.core/time but returns a vector of a the result of
  the code and the milliseconds rather than printing a string. Runs
  the code in an implicit do."
  [& body]
  `(let [start# (System/nanoTime)  ret# ~(cons 'do body)]
     [ret# (/ (double (- (System/nanoTime) start#)) 1000000.0)]))
