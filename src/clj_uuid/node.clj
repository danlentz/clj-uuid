(ns clj-uuid.node
  (:import [java.net         InetAddress
                             NetworkInterface]
           [java.sql         Timestamp]
           [java.security    MessageDigest]
           [java.util        Properties
                             Date]
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


(def node-id
  (memoize
   (fn []
     (let [addresses (all-local-addresses)
           ^MessageDigest digest (MessageDigest/getInstance "MD5")
           ^Properties props (System/getProperties)
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
         (map ub8) ubvec (#(subvec % 0 6)))))))


(def +node-id+          (node-id))

