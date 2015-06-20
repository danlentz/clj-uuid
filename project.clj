(defproject danlentz/clj-uuid "0.1.7-SNAPSHOT"
  :description  "A Clojure library for generation and utilization of
                UUIDs (Universally Unique Identifiers) as described by
                RFC-4122. This library extends the standard Java UUID class
                to provide true v1 (time based) and v3/v5 (namespace based)
                identifier generation. Additionally, a number of useful
                supporting utilities are provided to support serialization
                and manipulation of these UUIDs in a simple, efficient manner."
  :author       "Dan Lentz"
  :signing  {:gpg-key "B110BC3D"}
  :url          "https://github.com/danlentz/clj-uuid"
  :license      {:name "Eclipse Public License"
                  :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0" :scope "provided"]
                 [primitive-math      "0.1.4"]]
  :codox    {:output-dir  "doc/api"
             :src-dir-uri "https://github.com/danlentz/clj-uuid/blob/master/"
             :src-linenum-anchor-prefix "L"
             :project {:name "clj-uuid"}}
  :global-vars {*warn-on-reflection* false})
