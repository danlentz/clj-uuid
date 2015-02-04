(ns clj-uuid.node
  (:require [clj-uuid.bitmop :only [ubvec ub8]])
  (:import  [java.net         InetAddress NetworkInterface]
            [java.security    MessageDigest]
            [java.util        Properties]
            [java.nio.charset StandardCharsets]))


(defn all-local-addresses []
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


(defn- make-node-id-jdk6 []
  (clj-uuid.bitmop/ubvec
    (map clj-uuid.bitmop/ub8
      (.getHardwareAddress
        (java.net.NetworkInterface/getByInetAddress 
          (java.net.InetAddress/getLocalHost))))))


(defn- make-node-id-jdk7 []
  (let [addresses (all-local-addresses)
        ^MessageDigest digest (MessageDigest/getInstance "MD5")
        ^Properties    props  (System/getProperties)
        to-digest (reduce (fn [acc key]
                            (conj acc (.getProperty props key)))
                    addresses
                    ["java.vendor"
                     "java.vendor.url"
                     "java.version"
                     "os.arch"
                     "os.name"
                     "os.version"])]
    (doseq [^String d to-digest]
      (.update digest
        (.getBytes d StandardCharsets/UTF_8)))
    (->> (.digest digest)
      (map clj-uuid.bitmop/ub8)
      clj-uuid.bitmop/ubvec
      (#(subvec % 0 6)))))


(defn- make-node-id []
  (if (neg? (compare (System/getProperty "java.version") "1.7"))
    (make-node-id-jdk6)
    (make-node-id-jdk7)))

(def node-id   (memoize make-node-id))

(def +node-id+ (node-id))

