(ns financial-quant.yahoo-api
  (:require [clj-http.client :as client]
            [financial-quant.json :as json]))

(defonce cache-store (atom {}))

(comment
  (swap! cache-store assoc "my-cache-key" {:new 123})

  (get @cache-store "my-cache-key")

  cache-store


  (update {:x 3} :x inc))

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

;; (defn list-of-dates [ticker]
;;   (let [url (str )]))

(defn extract-option-chain [data]
  (let [result (get-in data [:optionChain :result 0 ])
        quote (:quote result)
        filtered-quote (select-keys quote [:bid :ask :shortName :symbol])
        options (get-in data [:optionChain :result 0 :options])
        calls (get-in options [0 :calls])
        puts (get-in options [0 :puts])
        filtered-calls (mapv required-keys calls)
        filtered-puts (mapv required-keys puts)
        transformed-map {:quote filtered-quote
                         :strikes (:strikes result)
                         :expirations (:expirationDates result)
                         :calls filtered-calls 
                         :puts filtered-puts}]   
    ;; (clojure.pprint/pprint data)
    
    transformed-map))

(defn fetch-option-data [ticker, date]
  (let [url (str "https://query1.finance.yahoo.com/v7/finance/options/" ticker)
        params {:formatted "true"
                :lang "en-US"
                :region "US"
                :date date ;; Unix timestamp
                }
        cache-key (str ticker "-" date)
        data (json/parse (or (get @cache-store cache-key)
                             (let [response (client/get url {:accept :json :query-params params})
                                   body (:body response)]
                               (swap! cache-store assoc cache-key body)
                               (println "fetching:" url params)
                               body)))]
     data))

(defn full-option-chain
  ([ticker] (full-option-chain ticker 20))
  ([ticker, exp-count]
   (let [origin (fetch-option-data ticker nil) 
         exp-dates (take exp-count (next (get-in origin [:optionChain :result 0 :expirationDates])))
         datas (into [(extract-option-chain origin)] 
                     (for [date exp-dates]
                       (extract-option-chain (fetch-option-data ticker date))))] 
     (reduce (fn [acc, item] 
               (assoc acc
                      ;; :quote (:quote acc)
                      :calls (concat (:calls acc) (:calls item))
                      :puts (concat (:puts acc) (:puts item))
                      )) 
             datas ))))

(defn limit-strikes [data, limit]
  (let [strikes (:strikes data)
        bid (get-in data [:quote :bid])
        upper-stack (take-last limit (take-while #(<= % bid) strikes))
        lower-stack (take limit (drop-while #(<= % bid) strikes))
        only-strikes (set (concat upper-stack lower-stack))
        filtered-calls (filter #(contains? only-strikes (:strike %)) (:calls data))
        filtered-puts (filter #(contains? only-strikes (:strike %)) (:puts data))]
    (assoc data 
           :calls filtered-calls 
           :puts filtered-puts)))

;;
(comment
  (for [x (range 0 1)]
    (* x 2))
  (reduce (fn [acc, item] 
            (assoc acc
                   ;; :quote (:quote acc)
                   :calls (concat (:calls acc) (:calls item))
                   :puts (concat (:puts acc) (:puts item))
                   )) 
          [{:quote {:price 10} :calls [{:strike 5} ]}
           {:quote {:price 12} :calls [{:strike 10} ]}] )


  (fetch-option-data "TSLA" nil)
  
  (def full (full-option-chain "TSLA"))
  (count (:calls full))
  (fetch-option-chain "TSLA")
  
  (-> (full-option-chain "TSLA" )
      (limit-strikes 10)
      :calls
      (#(map :strike %))
      distinct
      count)

)
