(ns clj-uuid.clock
  (:import [clojure.lang MapEntry]))
  

(set! *warn-on-reflection* true)

(def ^:const +tick-resolution+  9999)

(defn- pair ^MapEntry [k v]
  (MapEntry. ^short k ^long v))


(def state              (atom ^MapEntry (pair 0 0)))
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
    (let [^MapEntry new-state
          (swap! state
            (fn ^MapEntry [^MapEntry current-state]
              (loop [^long time-now (timestamp)]
                (if-not (= (.val current-state) time-now)
                  (pair 0 time-now)
                  (let [tt (.key current-state)]
                    (if (< tt +tick-resolution+)
                      (pair (inc tt) time-now)
                      (recur (timestamp))))))))]
      (+ (.key new-state) (.val new-state)))))


(set! *warn-on-reflection* false)
