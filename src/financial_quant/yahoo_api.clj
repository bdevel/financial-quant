(ns financial-quant.yahoo-api
  (:require [clj-http.client :as client]
            [financial-quant.json :as json]
            [financial-quant.cache :as cache]
            ))

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
        cache-key (str ticker "-" date ".json")
        data (json/parse (or (cache/lookup-today cache-key)
                             (let [response (client/get url {:accept :json :query-params params})
                                   body (:body response)]
                               ;;(write-in-db (assoc {} cache-key body))
                               (cache/write-daily cache-key body)
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


;;================================================================================
;; New stuff 2020-06-03


(defn accumulate-open-interest
  "Will update :calls with :total-open-interest which is the sum of all previous :expirations where :strike price is greater than the current item."
  [data]
  )

(comment
  ;; Goal:
  (-> (full-option-chain "TSLA" )
      (limit-strikes 10)
      (accumulate-open-interest)
      )
  )


(comment
  ;; (def full (full-option-chain "TSLA"))
  ;; (count (:calls full))
  ;; (fetch-option-chain "TSLA")

  ;; Get example call item
  (clojure.pprint/pprint (first (-> (full-option-chain "TSLA" )
                                    (limit-strikes 10)
                                    :calls
                                    )))


  (def samples [{
                :strike        100
                :expiration    1
                :open-interest 10
                 }
                {
                 :strike        100
                 :expiration    2
                 :open-interest 11
                 }
                {
                 :strike        100
                 :expiration    3
                 :open-interest 12
                 }
                
                {
                 :strike        200
                 :expiration    1
                 :open-interest 20
                 }
                {
                 :strike        200
                 :expiration    2
                 :open-interest 21
                 }
                {
                 :strike        200
                 :expiration    2
                 :open-interest 22
                 }])


  (defn accumulate-open-interest
    ""
    [contracts]
    ;; NOTE, This has an issue that it is only taking the previous :open-interest
    ;; and it should be taking the previous items :total-open-interest
    (loop [items  (sort-by :expiration contracts)
           out []]
      (let [;;[i n & others] items
            i        (first items)
            n        (second items)
            others   (next items)
            new-item (assoc i
                            :total-open-interest
                            (+ (:open-interest i 0)
                               (:open-interest n 0)))
            new-out  (conj out new-item)]
        (println "Working on " i " + " n)
        (if (empty? others)
          (sort-by :expiration new-out)
          (recur others new-out)))))

  (accumulate-open-interest samples)

  ;; example making a hashmap from a vector.
  ;;(into {} [[:a 1] [:b 2]])
  
  (let [groups (group-by :strike samples)
        v1 (into {} (map #(vector %1 (accumulate-open-interest %2))
                         (keys groups) (vals groups)))]
    
    (clojure.pprint/pprint samples)
    (clojure.pprint/pprint v1)
    )


)
