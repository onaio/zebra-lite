(ns ona.utils.dom
  (:require [dommy.core :as dommy])
  (:require-macros [dommy.macros :refer [sel sel1]]))

(defn click-fn [f]
  "Helper function to create a click function that prevents default"
  (fn [event] (.preventDefault event) (f)))

(defn by-id [id]
  "Helper function, shortcut for document.getElementById(id)"
  (.getElementById js/document id))

(defn add-listeners-to-collection
  [selector event-type action]
  (doseq [elem (sel selector)]
    (dommy/listen! elem event-type action)))

(defn hide-open-submenus []
  (doseq [submenu (sel :ul.submenu)]
    (let [parent (.-parentElement submenu)
          is-dropclick? (dommy/has-class? parent "drop-click")]
      (if is-dropclick?
        (dommy/set-style! submenu :visibility "hidden")))))

(defn init-search-dropdown
  [elem search-elem]
  ;; Dropdown with search
  (dommy/listen!
    (sel1 [elem :input]) :keyup
    (fn [event]
      (let [search-value (.toLowerCase (dommy/value (.-target event)))]
        (doseq [span-name (sel [:ul.submenu :li :a search-elem])]
          (let [proj-name (.toLowerCase (dommy/html span-name))
                list-item  (.-parentElement (.-parentElement span-name))]
            (dommy/show! list-item)
            (if (< (.indexOf proj-name search-value) 0)
              (dommy/hide! list-item))))))))

(defn init-dropdowns
  [div-class-name span-class-name]
  (doseq [dropdown (sel :.dropdown.drop-click)]
    (dommy/listen!
      dropdown :click
      (fn [event]
        (let [target (.-target event)
              next-sibling (.-nextElementSibling (.-parentElement target))
              is-submenu? (dommy/has-class? next-sibling "submenu")]
          (hide-open-submenus)
          (if is-submenu?
            (dommy/set-style! next-sibling :visibility "visible"))))))

  (dommy/listen!
    (sel1 :body) :click
    (fn [event]
      (let [dropdivs [(.. event -target) ; usually the icon
                      (.. event -target -parentNode) ; usually the grey div
                      (.. event -target -parentNode -parentNode)] ; dropdown
            dropdown-click? (some true? (map #(dommy/has-class? % "dropdown")
                                             dropdivs))]
        (when-not dropdown-click? (hide-open-submenus)))))

  (init-search-dropdown div-class-name span-class-name))

(defn new-container!
  "Returns a new div in the DOM, mostly used for rendering om components into.
   id is passed in or randomly generated, parent-selector is passed in or :body.
   Returns: HTMLDivElement."
  ([] (new-container! (str "container-" (gensym))))
  ([id] (new-container! id :body))
  ([id parent-selector]
   (let [div (.createElement js/document "div")
         meta (.createElement js/document "meta")]
     (.setAttribute div "id" id)
     (.setAttribute meta "name" "language-code")
     (dommy/append! (sel1 js/document parent-selector) meta div)
     div)))

(defn set-visibility
  "Set the CSS visibility property of a list of elements based on a boolean toggle
  false -> hidden, true -> visible."
  [visible? list-of-identifiers]
  (let [value (if visible? "visible" "hidden")]
    (doseq [id list-of-identifiers]
      (dommy/set-style! (sel1 id) :visibility value))))
