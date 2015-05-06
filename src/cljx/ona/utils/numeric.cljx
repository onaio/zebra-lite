(ns ona.utils.numeric
  (:require [clojure.string :refer [join]]
            #+cljs [ona.utils.interop :refer [format]]
            [inflections.core :refer [plural]]))

(defn pluralize-number
  "Create an appropriately pluralized string prefix by number."
  [number kind]
  (join " " [number (if (= 1 number) kind (plural kind))]))

(defn- divisor+suffix
  "Shorten the numerator by denominator with suffx."
  [numerator denominator suffix]
  (format "%.1f%s" (float (/ numerator denominator)) suffix))

(defn shorten-number
  "Shorten long counters to number-decimal format, e.g. 2000 = 2K"
  [number]
  (cond
   (< number 1000) (str number)
   (< number 1000000) (divisor+suffix number 1000 "K")
   :else (divisor+suffix number 1000000 "M")))
