(ns financial-quant.plots
  (:require [oz.core :as oz]
            [financial-quant.yahoo-api :as yahoo-api]))


(defn plot-values [api-data]
  (map 
    #(identity {
                :open-interest   (get % :open-interest) 
                :strike          (get % :strike) 
                :expiration      (*  1000 (get % :expiration))
                :last-price      (get % :last-price)
                :contract-symbol (get % :contract-symbol)
                }) 
    (:calls api-data)))

(defn make-open-interest-heatmap
  ""
  [api-data]
  (let [plot-data (plot-values sample-data)
        all-exp   (map :expiration plot-data)
        min-exp   (apply min all-exp)
        max-exp   (apply max all-exp)
        n-strikes (count (distinct (map :strike plot-data)))
        bid       (get-in api-data [:quote :bid])
        timeunit (if (> (count (distinct all-exp)) 20)
                   "yearmonthday"

                   "yearmonth")
        ]
   (println "n expr" (count (distinct all-exp)))
    {
     :$schema "https://vega.github.io/schema/vega-lite/v4.json",
     :width 800
     :height 800
     :title (str "Option Open Interest for " (get-in api-data [:quote :symbol]))
     :layer   [{
                :data     {:values plot-data},
                :mark     "rect",
                :encoding {
                           ;;:y     {:field :strike, :type "ordinal" :sort "ascending" },
                           :y     {:field :strike, :type "quantitative" :sort "ascending" :bin {:maxbins n-strikes }},

                           :x     {:field :expiration, :type "temporal" :timeUnit timeunit :bin false :axis {:labelAngle 90 :title "Expiration" }},
                           
                           :color {:aggregate "sum", :field :open-interest, :type "quantitative"}
                           },
                :config   {
                           :axis {:grid true, :tickBand "extent"}
                           }
                }
               {
                :data     {:values [{:price bid :date min-exp }
                                    {:price bid :date max-exp }]},
                :mark     "line",
                :encoding {
                           :x     {:field :date, :type "temporal" },
                           :y     {:field :price, :type "quantitative" :sort "ascending"},
                           :color {:value "#000000"}
                           },
                }
               ]
     
     }))


(def sample-data (-> (yahoo-api/full-option-chain "GOOG")
                     (yahoo-api/limit-strikes 50)))


(oz/view! (make-open-interest-heatmap sample-data))


(comment
  (sample-plot-values sample-data)
  (clojure.pprint/pprint (:strikes sample-data))

  (map :expiration (sample-plot-values sample-data))
  (get sample-data :calls)
  )

