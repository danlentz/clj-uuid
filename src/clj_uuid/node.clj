(ns clj-uuid.node
  (:require [clj-uuid.util   :refer [compile-if]])
  (:require [clj-uuid.bitmop :only  [ubvec ub8]])
  (:import  [java.net         InetAddress NetworkInterface]
            [java.security    MessageDigest]
            [java.util        Properties]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Internal NodeID Calculation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- all-local-addresses []
  (let [^InetAddress local-host (InetAddress/getLocalHost)
        host-name (.getCanonicalHostName local-host)
        base-addresses #{(str local-host) host-name}
        network-interfaces (reduce (fn [acc ^NetworkInterface ni]
                                     (apply conj acc
                                       (map str (enumeration-seq
                                                  (.getInetAddresses ni)))))
                             base-addresses
                             (enumeration-seq
                               (NetworkInterface/getNetworkInterfaces)))]
    (reduce conj network-interfaces
      (map str (InetAddress/getAllByName host-name)))))


(compile-if (neg? (compare (System/getProperty "java.version") "1.7"))
  ;; JDK6
  (defn- make-node-id []
    (clj-uuid.bitmop/ubvec
      (map clj-uuid.bitmop/ub8
        (repeatedly 6 #(rand-int 256)))))
        
    ;; (clj-uuid.bitmop/ubvec
    ;;   (map clj-uuid.bitmop/ub8
    ;;     (.getHardwareAddress
    ;;       (java.net.NetworkInterface/getByInetAddress 
    ;;         (java.net.InetAddress/getLocalHost))))))

  ;; JDK7
  (defn- make-node-id []
    (let [addresses (all-local-addresses)
          ^MessageDigest digest (MessageDigest/getInstance "MD5")
          ^Properties    props  (System/getProperties)
          to-digest (reduce (fn [acc key]
                              (conj acc (.getProperty props key)))
                      addresses ["java.vendor" "java.vendor.url"
                                 "java.version" "os.arch"
                                 "os.name" "os.version"])]
      (doseq [^String d to-digest]
        (.update digest
          (.getBytes d java.nio.charset.StandardCharsets/UTF_8)))
      (->> (.digest digest)
        (map clj-uuid.bitmop/ub8)
        clj-uuid.bitmop/ubvec
        (#(subvec % 0 6))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public NodeID API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def node-id   (memoize make-node-id))

(def +node-id+ (node-id))

