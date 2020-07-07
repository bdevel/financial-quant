(ns financial-quant.core
  (:gen-class)
  (:require [oz.core :as oz]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (oz/start-server!))

(comment
  (-main)
  )
