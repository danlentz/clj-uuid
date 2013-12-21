(defproject danlentz/clj-uuid "0.0.6-SNAPSHOT"
  :description  "A Clojure library for generation and utilization of
                UUIDs (Universally Unique Identifiers) as described by
                RFC-4122. This library extends the standard Java UUID class
                to provide true v1 (time based) and v3/v5 (namespace based)
                identifier generation. Additionally, a number of useful
                supporting utilities are provided to support serialization
                and manipulation of these UUIDs in a simple, efficient manner.

                The essential nature of the value RFC4122 UUIDs provide
                is that of an enormous namespace and a deterministic
                mathematical model by means of which one navigates
                it. UUIDs represent an extremely powerful and versatile
                computation technique that is often overlooked, and
                underutilized. In my opinion, this, in part, is due to
                the generally poor quality, performance, and capability
                of available libraries and, in part, due to a general
                misunderstanding in the popular consiousness of their
                proper use and benefit. It is my hope that this library
                will serve to expand awareness, make available, and
                simplify use of RFC4122 identifiers to a wider
                audience."
  :url          "http://github.com/danlentz/clj-uuid/"
  :license      {:name "Eclipse Public License"
                  :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure                "1.5.1"]
                 [org.codehaus.jsr166-mirror/jsr166y "1.7.0"]
                 [primitive-math                     "0.1.3"]])
