(defproject neuhon "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [commons-io "2.5"]
                 [org.clojure/data.csv "0.1.3"]
                 [net.mikera/core.matrix "0.58.0"]]
  :javac-options {:destdir "classes/"}
  :java-source-path "src/neuhon"
  :disable-implicit-clean true)