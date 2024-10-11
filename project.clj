(defproject danlentz/danlentz.clj-uuid "0.2.0"
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
  :signing  {:gpg-key "D0540BEC1EA1D3D0"}
  :url          "https://github.com/danlentz/danlentz.clj-uuid"
  :license      {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.12.0" :scope "provided"]
                 [org.clj-commons/primitive-math "1.0.1"]]
  :codox    {:output-path  "doc/api"
             :src-dir-uri  "https://github.com/danlentz/danlentz.clj-uuid/blob/master/"
             :doc-files []
             :src-linenum-anchor-prefix "L"
             :project {:name "danlentz.clj-uuid"}}
  :global-vars {*warn-on-reflection* true})
