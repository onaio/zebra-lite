(ns ona.utils.string
  (:use [clojure.string :only [blank? capitalize join split]]))

;; validation regexes
(def email-regex #"(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,6}$")
(def file-extension-regex #"\.([^.]+)$")
(def twitter-username-regex #"^[A-Za-z0-9_]*$")

;; truncation variables
(def truncate-if-longer-than 50)
(def ellipsis-start 36)
(def ellipsis-stop-from-end 12)

(def not-empty? (complement empty?))

(defn truncate-with-ellipsis
  "Shorten a string to a certain length with middle ellipsis."
  [string]
  (if (> (count string) truncate-if-longer-than)
    (let [end-start (- (count string) ellipsis-stop-from-end)]
      (str
       (subs string 0 ellipsis-start)
       "..."
       (subs string end-start)))
    string))

(defn first-cap
  "Return the first character of a string capitalized."
  [string]
  (-> string first str capitalize))

(defn ^boolean substring?
  "True if substring is a substring of string"
  [substring string]
  ((complement nil?) (re-find (re-pattern substring) string)))

(defn ^boolean is-email?
  "True if string is an email address."
  [string]
  #+clj (re-matches email-regex string)
  #+cljs (first (.match string email-regex)))

(defn ^boolean is-twitter-username?
  "True if string is a valid twitter username"
  [string]
  #+clj (re-matches twitter-username-regex string)
  #+cljs (first (.match string twitter-username-regex)))

(defn postfix-paren-count
  "Wrap the count of a collection in parens and postfix."
  [prefix collection]
  (str prefix " (" (count collection) ")"))

(defn ^boolean ends-with?
  "True if string ends with the passed suffix."
  [string suffix]
  (let [offset (- (count string) (count suffix))]
    (and (>= offset 0)
         (= suffix (subs string offset)))))

(defn safe-blank?
  [string]
  (-> string str blank?))
