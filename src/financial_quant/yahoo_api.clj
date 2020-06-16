(ns financial-quant.yahoo-api
  (:require [clj-http.client :as client]
            [financial-quant.json :as json]))

(defn required-keys [args]
  {:expiration (get-in args [:expiration :raw])
   :strike (get-in args [:strike :raw])
   :in-the-money (get args :inTheMoney)
   :ask (get-in args [:ask :raw])
   :bid (get-in args [:bid :raw])
   :volume (get-in args [:volume :raw])
   :open-interest (get-in args [:openInterest :raw])
   :contract-symbol (get args :contractSymbol)
   :last-price (get-in args [:lastPrice :raw])})

(defn fetch-option-chain [ticker]
  (let [url (str "https://query1.finance.yahoo.com/v7/finance/options/" ticker)
        response (client/get url {:accept :json :query-params {:formatted "true"
                                                               :lang "en-US"
                                                               :region "US"
                                                               :date "1591920000"}})
        body (:body response)
        data (json/parse body)
        quote (get-in data [:optionChain :result 0 :quote])
        filtered-quote (select-keys quote [:bid :ask])
        options (get-in data [:optionChain :result 0 :options])
        calls (get-in options [0 :calls])
        puts (get-in options [0 :puts])
        filtered-calls (mapv required-keys calls)
        filtered-puts (mapv required-keys puts)
        transformed-map {:quote filtered-quote :calls filtered-calls :puts filtered-puts}]
    transformed-map))


;;
(comment

  (fetch-option-chain "TSLA"))

