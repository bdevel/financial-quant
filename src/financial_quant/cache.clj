 (ns financial-quant.cache
  (:require
   [clojure.java.io :as io]
   [java-time :as t]))


(defmacro nil-on-exception [& forms]
  `(try ~@forms
        (catch Exception e# 
          (println "ERROR: " (.getMessage e#))
          nil)))

(comment 
  (nil-on-exception (slurp "non-existing.txt"))
  
)

(def dir-path "cache")

(defn write [cache-key, data]
  (let [file-name (str dir-path "/" cache-key)]
    (io/make-parents file-name)
    (spit file-name data)))

(defn write-daily [cache-key,data]
  (write (str (t/local-date) "/" cache-key) data))

(defn lookup [cache-key]
  (nil-on-exception (slurp (str dir-path "/" cache-key))))

(defn lookup-today [cache-key]
    (nil-on-exception (slurp (str dir-path "/" (t/local-date) "/" cache-key))))

(defn lookup-date [cache-key, date]
  (nil-on-exception (slurp (str dir-path "/" date "/" cache-key))))

(comment 
  (write "TSLA.edn" {:a "we"})
  (lookup "TSLAx.edn")
  (lookup-today "TSLA.edn")
  (lookup-date "TSLA.edn" "2020-06-6")

)
