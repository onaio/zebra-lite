(ns ona.utils.collections)

(defn in?
  "True if elem is in list, false otherwise."
  [list elem]
  (boolean (some #(= elem %) list)))