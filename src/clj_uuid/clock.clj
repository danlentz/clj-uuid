(ns clj-uuid.clock
  (:use [clojure.core])
  (:use [clojure.pprint])
  (:use [clojure.repl :only [doc find-doc apropos]])
  (:use [clj-uuid.constants])
  (:use [clj-uuid.bitmop]))


(defn make-node-id []
  (svec
    (map ub8
      (.getHardwareAddress
        (java.net.NetworkInterface/getByInetAddress 
          (java.net.InetAddress/getLocalHost))))))
  
(def +node-id+          (make-node-id))
(def +tick-resolution+   0x4000)
(def +startup-nanotime+ (System/nanoTime))

(def uuids-this-tick (atom 0))
(def last-time       (atom 0))
(def clock-seq       (atom (+ (rand-int 9999) 1)))

(defn get-internal-real-time []
  (/ (- (System/nanoTime) +startup-nanotime+) 1000))

(defn get-universal-time []
  (quot (System/currentTimeMillis) 1000))

(defn make-timestamp []
  (letfn [(ts []
            (+ (* (get-universal-time) 10000000) 100103040000000000))]
    (loop [time-now (ts)]
      (if (not (= @last-time time-now))
        (do
          (swap! uuids-this-tick (constantly 0))
          (swap! last-time (constantly time-now))
          time-now)
        (if (< @uuids-this-tick +tick-resolution+)
          (+ time-now (swap! uuids-this-tick inc))
          (recur (ts)))))))

