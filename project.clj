(defproject financial-quant "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-http "3.10.1"]
                 [cheshire "5.8.1"]
                 [clojure.java-time "0.3.2"]
                 [metasoarous/oz "1.6.0-alpha6"]]
  :main ^:skip-aot financial-quant.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
