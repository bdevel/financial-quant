(ns financial-quant.plots
  (:require [oz.core :as oz]
            [financial-quant.yahoo-api :as yahoo-api]
            [financial-quant.historic-quote :as historic]
            ))


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


(defn make-total-open-interest-heatmap
  ""
  [api-data]
  (let [plot-data (plot-values api-data)
        all-exp   (map :expiration plot-data)
        min-exp   (apply min all-exp)
        max-exp   (apply max all-exp)
        n-strikes (count (distinct (map :strike plot-data)))
        bid       (get-in api-data [:quote :bid])
        timeunit  (if (> (count (distinct all-exp)) 20)
                    "yearmonthday"
                    "yearmonth")
        ]
   (println "n expr" (count (distinct all-exp)))
    {
     :$schema "https://vega.github.io/schema/vega-lite/v4.json",
     :width 800
     :height 800
     :title (str "Option Total Open Interest for " (get-in api-data [:quote :symbol]))
     :layer   [{
                :data     {:values plot-data},
                :mark     "rect",
                :encoding {
                           ;;:y     {:field :strike, :type "ordinal" :sort "ascending" },
                           :y     {:field :strike, :type "quantitative" :sort "ascending" :bin {:maxbins n-strikes }},

                           :x     {:field :expiration, :type "temporal" :timeUnit timeunit :bin false :axis {:labelAngle 90 :title "Expiration" }},
                           
                           :color {:aggregate "sum", :field :total-open-interest, :type "quantitative"}
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


(defn make-historic-quote-plot
  ""
  [quotes]
  (let [_ nil]
    ;; (*  1000 (get % :expiration))
    {
     :$schema  "https://vega.github.io/schema/vega-lite/v4.json",
     :width    800
     :height   800
     :title    (str "Historic Quotes " ) ;; (get-in api-data [:quote :symbol]))
     :data     {:values quotes}
     :encoding {
                :x     {
                        :field :timestamp
                        :type  "temporal"
                        :title "Date in 2009"
                        :axis  {
                                :format     "%m/%d",
                                :labelAngle -45,
                                :title      "The title"
                                }
                        },
                :color {
                        :condition {
                                    :test  "datum.open < datum.close"
                                    :value "#06982d"
                                    },
                        :value     "#ae1325"
                        }
                },
     :layer    [
                {
                 :mark     "rule",
                 :encoding {
                            :y {
                                :field :low
                                :type  "quantitative",
                                :scale {:zero false},
                                :title "Price"

                                },
                            :y2 {:field :high}
                            }
                 },
                {
                 :mark     "bar",
                 :encoding {
                            :y  {:field :open :type "quantitative"},
                            :y2 {:field :close}
                            }
                 }
                ]
     
     }))






(comment
  (def open-interest-data
    (-> (yahoo-api/full-option-chain "TSLA")
        (yahoo-api/limit-strikes 50)
        (yahoo-api/accumulate-open-interest)))
  (oz/view! (make-total-open-interest-heatmap open-interest-data))

  
  )

(comment 
  (oz/view! (make-historic-quote-plot (historic/extract-historic-quotes (historic/fetch-historic-quote-data "TSLA" "123123123"))))

  )


(comment
  (sample-plot-values sample-data)
  (clojure.pprint/pprint (:strikes sample-data))

  (map :expiration (sample-plot-values sample-data))
  (get sample-data :calls)
  )

