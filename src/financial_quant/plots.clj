(ns financial-quant.plots
  (:require [oz.core :as oz]
            [financial-quant.yahoo-api :as yahoo-api]))

(def sample-data (yahoo-api/full-option-chain "TSLA"))

(defn sample-plot-values [api-data]
  (map 
   #(identity {
               :open-interest (get % :open-interest) 
               :strike (get % :strike)
               :expiration (get % :expiration)
               :last-price (get % :last-price)
               :contract-symbol (get % :contract-symbol)
               }) 
   (:calls api-data)))

(def sample-plot 
 {
  :$schema "https://vega.github.io/schema/vega-lite/v4.json",
  :data {:values (sample-plot-values sample-data)},
  :mark "rect",
  :encoding {
    :y {:field :strike
, :type "nominal"},
    :x {:field :expiration, :type "ordinal"},
    :color {:aggregate "mean", :field :open-interest, :type "quantitative"}
  },
  :config {

    :axis {:grid true, :tickBand "extent"}
  }
})


(oz/view! sample-plot)


(comment
  (sample-plot-values sample-data)

  (map :expiration (sample-plot-values sample-data))
  (get sample-data :calls))


