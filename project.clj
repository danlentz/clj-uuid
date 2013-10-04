(defproject clj-uuid "0.1.0-SNAPSHOT"
  :description  "A Clojure library for generation and utilization of
                UUIDs (Universally Unique Identifiers) as described by
                RFC-4122.  The essential nature of the service it provides
                is that of an enormous _namespace_ and a deterministic
                mathematical model by means of which it may be precisely
                and efficiently navigated."
  
  :url          "http://github.com/danlentz/clj-uuid"

  :license      {:name "Eclipse Public License"
                  :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [[org.clojure/clojure "1.5.1"]
                  [primitive-math     "0.1.3"]
                  [byte-streams       "0.1.5"]
                  [byte-transforms    "0.1.0"]])

;;                  [org.bovinegenius/exploding-fish "0.3.3"]])
