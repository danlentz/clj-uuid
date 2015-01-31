(ns clj-uuid.clock)
  

(set! *warn-on-reflection* true)

(def ^:const +tick-resolution+  9999)

(deftype Pair [^short k ^long v])

(defn- pair ^Pair [k v]
  (Pair. k v))


(def state              (atom (pair 0 0)))
(def clock-seq          (atom (+ (rand-int 9999) 1)))


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
    (let [^Pair new-state
          (swap! state
            (fn [^Pair current-state]
              (loop [^long time-now (timestamp)]
                (if-not (= (.v current-state) time-now)
                  (pair 0 time-now)
                  (let [tt (.k current-state)]
                    (if (< tt +tick-resolution+)
                      (pair (inc tt) time-now)
                      (recur (timestamp))))))))]
      (+ (.k new-state) (.v new-state)))))


(set! *warn-on-reflection* false)
