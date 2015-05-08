(ns ona.utils.interop
  (:require [cognitect.transit :as t]
            [goog.string.format]
            [goog.string]))

(defn console-log [s]
  "calls console.log after converting a cljs object to js."
  (.log js/console (clj->js s)))

(defn format
  "Formats a string using goog.string.format, so we can use format in cljx."
  [fmt & args]
  (apply goog.string/format fmt args))

(defn redirect-to-url! [url]
  "Re-directs the page to the given url."
  (set! (.-location js/window) url))

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

(defn json->cljs [s]
  "Convert json string to cljs object using transit.
   Fast, but doesn't preserve keywords."
  (t/read (t/reader :json) s))

(defn json->js [s]
  "Convert json to js using JSON.parse"
  (.parse js/JSON s))

(defn json->js->cljs [s]
  "Convert json string to cljs via js.
   Slow method, but preserves keywords, and appropriate for small json."
  (js->clj (json->js s) :keywordize-keys true))

(defn format
  "Formats a string using goog.string.format, so we can use format in cljx."
  [fmt & args]
  (apply goog.string/format fmt args))
