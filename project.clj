(defproject danlentz/clj-uuid "0.2.5-SNAPSHOT"
  :description "A Clojure library for generation and utilization of
                UUIDs (Universally Unique Identifiers) as described by
                RFC-9562. This library extends the standard Java
                UUID class to provide true v1, v6, v7 (time based) and
                v3/v5 (namespace based), and v8 (user customizable)
                identifier generation. Additionally, a number of useful
                utilities are provided to support serialization and
                manipulation of these UUIDs in a simple, efficient
                manner."
  :author       "Dan Lentz"
  :jvm-opts ^:replace []
  :signing  {:gpg-key "0CA466A1AB48F0C0264AF55307BAD70176C4B179"}
  :url          "https://github.com/danlentz/clj-uuid"
  :license      {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.12.0" :scope "provided"]
                 [org.clj-commons/primitive-math "1.0.1"]]
  :plugins  [[lein-cloverage "1.2.4"]
             [lein-codox "0.10.8"]]
  :cloverage {:test-ns-regex [#"clj-uuid\.(?!bench|compare-bench).*"]}
  :profiles {:test {:dependencies
                    [[com.fasterxml.uuid/java-uuid-generator "5.2.0"]
                     [com.github.f4b6a3/uuid-creator "6.1.1"]]}}
  :codox    {:output-path  "doc/api"
             :src-dir-uri  "https://github.com/danlentz/clj-uuid/blob/master/"
             :doc-files []
             :src-linenum-anchor-prefix "L"
             :project {:name "clj-uuid"}}
  :global-vars {*warn-on-reflection* true})
