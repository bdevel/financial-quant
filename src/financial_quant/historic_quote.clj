(ns financial-quant.historic-quote
  (:require [financial-quant.json :as json]
            [financial-quant.cache :as cache]
            [clj-http.client :as client]
            [java-time :as t]))

(defn extract-historic-quotes [data]
  (let [result (get-in data [:chart :result 0])
        timestamps (get result :timestamp)
        open (get-in result [:indicators :quote 0 :open])
        high (get-in result [:indicators :quote 0 :high])
        low (get-in result [:indicators :quote 0 :low])
        close (get-in result [:indicators :quote 0 :close])
        volume (get-in result [:indicators :quote 0 :volume])
        transformed-map (map (fn [a b c d e f] {:timestamp a :open b :high c :low d :close e :volume f}) timestamps open high low close volume)]
    (println (count timestamps) 
             (count open) 
             (count high) 
             (count low) 
             (count close) 
             (count volume))
    transformed-map))

(def valid-ranges
  ["1d",
   "5d",
   "1mo",
   "3mo",
   "6mo",
   "1y",
   "2y",
   "5y",
   "10y",
   "ytd",
   "max"
   ])


;; See https://github.com/dm3/clojure.java-time
;; (str (t/local-date))

(defn fetch-historic-quote-data [ticker, date]
  (let [url       (str "https://query1.finance.yahoo.com/v8/finance/chart/" ticker)
        params    {:lang "en-US"
                   :region "US"
                   :includePrePost true
                   :period1 "1552546800"
                   :period2 "1591713732"
                   :interval "1d"
                   :symbol ticker}
        cache-key (str ticker "-" date)
        data      (json/parse (or (cache/lookup cache-key)
                                  (let [response (client/get url {:accept :json :query-params params})
                                        body (:body response)]
                                    (cache/write-daily cache-key body)
                                    (println "Fetching: " url params)
                                    body)))]
    data))


(comment 
  (extract-historic-quotes (fetch-historic-quote-data "TSLA" "123123123"))
  
  (def quotes (extract-historic-quotes (fetch-historic-quote-data "TSLA" "123123123")))

  (clojure.pprint/pprint quotes)
  
)



