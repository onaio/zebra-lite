(ns ona.utils.seq
  (:require [clojure.set :as cset]
            [ona.utils.string :refer [safe-blank?]]))

(defn diff
  "Return difference between 2 sequences."
  [a b]
  (cset/difference (set a) (set b)))

(defn ordered-diff
  "Return difference between 2 sequences. Preserves ordering in first seq."
  [a b]
  (filter #(not (contains? (set b) %)) a))

(defn union
  "Merges two sequeneces"
  [a b]
  (cset/union (set a) (set b)))

(defn has-keys?
  "True is map has all these keys."
  [m keys]
  (every? (partial contains? m) keys))

(defn in?
  "True if elem is in list, false otherwise."
  [list elem]
  (boolean (some #(= elem %) list)))

(defn remove-nil
  "Remove nil values from a sequence."
  [l]
  (remove nil? l))

(defn toggle [coll x]
  "Removes x from coll if present, and adds if absent."
  (if (contains? (set coll) x)
    (remove #(= x %) coll)
    (conj coll x)))

(defn indexed [coll]
  "Given a seq, produces a two-el seq. [a b c] => [[0 a] [1 b] [2 c]]."
  (map-indexed vector coll))

(def select-values (comp vals select-keys))

(def select-value (comp first select-values))

(defn remove-falsey-values
  "Remove map entries where the value is falsey."
  [a-map]
  (into {} (remove (comp safe-blank? second) a-map)))
