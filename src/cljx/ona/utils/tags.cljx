(ns ona.utils.tags)

(defn image
  "Return a path to a local image."
  [path]
  (str "/img/" path))

(defn include-js
  "Incude a path to a JavaScript file."
  [path & opts]
  (let [{:keys [no-prefix] :as opts-hash} (apply hash-map opts)
        src (if no-prefix path (str "/js/" path))]
    [:script (merge {:src src :type "text/javascript"}
                    (dissoc opts-hash :no-prefix))]))

(defn js-tag
  "Create a JavaScript tag with content."
  [content]
  [:script {:type "text/javascript"} content])

(defn js-submit
  "Build string to submit form via JavaScript."
  [form-id]
  (str "javascript:document.forms[\"" form-id "\"].submit();"))
