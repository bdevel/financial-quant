(ns financial-quant.plots
  (:require [oz.core :as oz]
            [financial-quant.yahoo-api :as yahoo-api]
            [financial-quant.historic-quote :as historic]
            ))


(defn plot-values [api-data]
  (map 
    #(identity {
                :open-interest   (get % :open-interest) 
                :total-open-interest   (get % :total-open-interest) 
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
    {
     :$schema "https://vega.github.io/schema/vega-lite/v4.json",
     :width 800
     :height 800
     :title (str "Option Total Open Interest for " (get-in api-data [:quote :symbol]))
     :layer   [{
                :data     {:values plot-data},
                ;;:mark     "rect",
                :mark {:type "rect", :tooltip true},
                :encoding {
                            :y     {:field :strike, :type "ordinal" :sort "descending" },
                           ;;:y     {:field :strike, :type "quantitative" :sort "ascending" :bin {:maxbins n-strikes }},

                           ;;:x     {:field :expiration, :type "ordinal"} ;; :timeUnit timeunit :bin false :axis {:labelAngle 90 :title "Expiration" }},
                           :x     {:field :expiration, :type "temporal" :timeUnit timeunit :bin false :axis {:labelAngle 90 :title "Expiration" }},
                           
                           :color {:field :total-open-interest, :type "quantitative"}
                           ;;:color {:aggregate "sum", :field :total-open-interest, :type "quantitative"}
                           },
                :config   {
                           :axis {:grid true, :tickBand "extent"}
                           }
                }
               #_{
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

(defn make-open-interest-heatmap
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
   
    {
     :$schema "https://vega.github.io/schema/vega-lite/v4.json",
     :width 800
     :height 800
     :title (str "Option Open Interest for " (get-in api-data [:quote :symbol]))
     :layer   [{
                :data     {:values plot-data},
                ;;:mark     "rect",
                :mark {:type "rect", :tooltip true},
                :encoding {
                           :y     {:field :strike, :type "ordinal" :sort "descending" },
                           ;;:y     {:field :strike, :type "quantitative" :sort "ascending" :bin {:maxbins n-strikes }},

                           :x     {:field :expiration, :type "temporal" :timeUnit timeunit :bin false :axis {:labelAngle 90 :title "Expiration" }},
                           ;;:x     {:field :expiration, :type "ordinal" };;:timeUnit timeunit :bin false :axis {:labelAngle 90 :title "Expiration" }},
                           
                           ;;:color {:aggregate "sum", :field :open-interest, :type "quantitative"}
                           :color {:field :open-interest, :type "quantitative"}
                           },
                :config   {
                           :axis {:grid true, :tickBand "extent"}
                           }
                }
               #_{
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




(let [open-interest-data  (-> (yahoo-api/full-option-chain "TSLA")
                              ;;(yahoo-api/limit-strikes 100)
                              (yahoo-api/accumulate-open-interest))
      total-interest-plot (make-total-open-interest-heatmap open-interest-data)
      open-interest-plot  (make-open-interest-heatmap open-interest-data)]


  (clojure.pprint/pprint
    (map #(select-keys % [:open-interest :total-open-interest :strike])
         (filter #(and (= (:expiration %) 1594339200) (> (:strike %) 1200) (< (:strike %) 1400))
                 (map #(select-keys % [:open-interest :total-open-interest :strike :expiration])
                      (:calls open-interest-data)))))
  
  (oz/view! [:div
             [:h1 "Look ye and behold"]
             [:p "A couple of charts"]
             [:div {:style {:display "block" }}
              [:vega-lite total-interest-plot]]
             [:div {:style {:display "block" }}
              [:vega-lite open-interest-plot]]
             
             [:h2 "Great job!"]
             ]
            ))


(comment
  
  (oz/view! (make-historic-quote-plot (historic/extract-historic-quotes (historic/fetch-historic-quote-data "TSLA" "123123123"))))

  )


(comment
  (sample-plot-values sample-data)
  (clojure.pprint/pprint (:strikes sample-data))

  (map :expiration (sample-plot-values sample-data))
  (get sample-data :calls)
  )

