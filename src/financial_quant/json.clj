(ns financial-quant.json
  (:require [cheshire.core :as j]
            [cheshire.generate :refer [add-encoder]]))

(defn not-episolon
  "Prevent using 31E9 format"
  [v generator]
  (.writeNumber generator
                (clojure.string/replace (format "%.10f" v) #"0+$" "")))


(defn iso8601
  "Format for JSON dates"
  [v generator]
  (.writeString generator
                (str v)))

(add-encoder java.lang.Double not-episolon)
(add-encoder java.lang.Float not-episolon)
(add-encoder java.time.LocalDateTime iso8601)
;;(add-encoder org.joda.time.DateTime iso8601)


(defn key-out
  "Convers clojure keys to json keys"
  [k]
  (clojure.string/replace (name k) "-" "_"))

(defn key-in
  "converter for json keys to clojure keys"
  [k]
  (keyword (clojure.string/replace k "_" "-")))


(defn pretty
  ""
  [item]
  (j/generate-string item {:key-fn key-out :pretty true}))


(defn pprint
  ""
  [item]
  (println (pretty item)))

(defn dump
  ""
  [item]
  (j/generate-string item {:key-fn key-out}
 )) ;; :value-fn val-out


(defn parse
  ""
  [text]
  (if (clojure.string/blank? text)
    {} ;;NOTE, this is reverse of
    (j/parse-string text  key-in)))


(comment
  (parse "{\"foo_bar\": 13}")
  (dump {:foo-bar 123})
  ;; Should add the E
  (dump {:unix 1.5646258437873077E9}) ;; {"unix":1564625843.7873077}

  (dump {:created-at (clj-time.core/now)})
  (j/parse-string "{\"foo\":\"bar\"}" true)

  )

