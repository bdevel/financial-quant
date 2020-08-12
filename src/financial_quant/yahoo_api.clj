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
                               (println "Fetching: " url params)
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

(defn acc-open-interest
    "Add up interest by strike price"
    [contracts]
  (let [sorted-items (reverse (sort-by :expiration contracts))
          acc-score    (reduce 
                         (fn [a i] 
                           (conj a (assoc i :total-open-interest (+ (or (:total-open-interest (last a)) 0)
                                                                    (or (:open-interest i) 0))))) 
                         [] 
                         sorted-items)]
      acc-score))

(defn fill-strikes-and-expirations
  "Assuming all contracts have the same expiration, start at lowest strike, if a strike missing then use the previous value of attr to fill gap."
  [contracts]
  (let [;;sorted-items (sort-by :strike contracts)
        all-strikes (distinct (map :strike contracts))
        all-expirations (distinct (map :expiration contracts))

        existing (reduce (fn [acc c]
                           (assoc-in acc [(:strike c) (:expiration c)] true))
                         {} contracts)
        all-combos (mapcat (fn [s]
                                    (map (fn [x]
                                           (vector s x))
                                         all-expirations))
                                  all-strikes)
        missing (reduce (fn [acc [strike exp]]
                          (if (get-in existing [strike exp])
                            acc
                            (conj acc {:strike strike :expiration exp
                                       :open-interest 0})))
                        (list)
                        all-combos)
        
        ]
    (into contracts missing)))

(comment
  (fill-strikes-and-expirations [{:strike 1 :expiration 10}
                                 {:strike 2 :expiration 20}]
                          )
  )

(defn acc-open-interest-by-expiration
  "Add up columns for heatmap"
  [contracts]
  (let [sorted-items (sort-by :strike contracts) ;; for :calls, sort ascending
        acc-score    (reduce 
                       (fn [a i] 
                         (conj a (assoc i :total-open-interest (+ (or (:total-open-interest (last a)) 0)
                                                                  (or (:total-open-interest i) 0))))) 
                       [] 
                       sorted-items)]
    acc-score))


(defn accumulate-open-interest
  "Will update :calls with :total-open-interest which is the sum of all previous :expirations where :strike price is greater than the current item."
  [data]
  (let [calls  (fill-strikes-and-expirations (:calls data))
        groups (group-by :strike calls)
        v1     (apply concat [] (map acc-open-interest (vals groups)))
        v2     (apply concat [] (map acc-open-interest-by-expiration (vals (group-by :expiration v1))))]
    
    (assoc data :calls v2)))

(comment
  ;; Goal:
  (-> (full-option-chain "TSLA" )
      (limit-strikes 10)
      (accumulate-open-interest)
      )
  )


(comment
;;   ;; (def full (full-option-chain "TSLA"))
;;   ;; (count (:calls full))
;;   ;; (fetch-option-chain "TSLA")

;;   ;; Get example call item
;;   (clojure.pprint/pprint (first (-> (full-option-chain "TSLA" )
;;                                     (limit-strikes 10)
;;                                     :calls
;;                                     )))


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
                 :expiration    3
                 :open-interest 22
                 }])

  (let [v (reverse (sort-by :strike (:calls (accumulate-open-interest {:calls samples}))))
        vg (vals (group-by :strike v)) #_(map #(:total-open-interest %) )
        vv (map (fn [g]
                  ;;(map #(select-keys % [:strike :total-open-interest :expiration]) g)
                  (map :total-open-interest g))
                vg)]
    (println "=====================")
    (run! println vv))
  

  ;; samples:
  [20 21 22
   10 11 12]

  ;; acc rows, right to left
  [63 43 22
   33 23 12]

  ;; then, acc cols bottom to top for :calls, goal:
  [96 66 34
   33 23 12]
  
  )
