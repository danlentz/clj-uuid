(ns clj-uuid.clock)

(set! *warn-on-reflection* true)

(def ^:const +tick-resolution+  9999)

(defn- pair ^clojure.lang.MapEntry [k v]
  (clojure.lang.MapEntry. ^short k ^long v))


(def state              (atom ^clojure.lang.MapEntry (pair 0 0)))
(def clock-seq          (atom ^short (+ (rand-int 9999) 1)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; we pad to achieve the rfc4122 compat and support atomic incremental
;; uuids-this-second subcounter on the low order bits, guarantee that never
;; collides regardless of clock precision:
;;
;; 113914335216380000  = (+ (* (get-epoch-time)   10000)    100103040000000000)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn monotonic-time []
  (letfn [(timestamp []
            (+ (* (System/currentTimeMillis) 10000)  100103040000000000))]
    (apply +
      (swap! state
        (fn ^clojure.lang.MapEntry [^clojure.lang.MapEntry current-state]
          (loop [^long time-now (timestamp)]
            (if-not (= (.val current-state) time-now)
              (pair 0 time-now)
              (let [tt (.key current-state)]
                (if (< tt +tick-resolution+)
                  (pair (inc tt) time-now)
                  (recur (timestamp)))))))))))


(set! *warn-on-reflection* false)
