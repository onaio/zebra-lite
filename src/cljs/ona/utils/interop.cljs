(ns ona.utils.interop
  (:require [cognitect.transit :as t]
            [goog.string.format]
            [goog.string]))

(defn format
  "Formats a string using goog.string.format, so we can use format in cljx."
  [fmt & args]
  (apply goog.string/format fmt args))

(defn safe-regex [s & {:keys [:ignore-case?]
                       :or    {:ignore-case? true}}]
  "Create a safe (escaped) js regex out of a string.
   By default, creates regex with ignore case option."
  (let [s (.replace s
                    #"/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/"
                    "\\$&")] ;; See http://stackoverflow.com/a/6969486
    (if :ignore-case?
      (js/RegExp. s "i")
      (js/RegExp. s))))