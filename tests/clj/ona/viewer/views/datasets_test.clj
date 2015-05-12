(ns ona.viewer.views.datasets-test
  (:require [midje.sweet :refer :all]
            [ona.viewer.views.datasets :refer :all]))

(def username "testusername")
(def temp-token "a1b2c3")
(def account {:username username :temp_token temp-token})
(def dataset-id "1")

(facts "About all"
       (all account) => (every-checker
                          (contains "Loading Forms...")
                          (contains (format
                                      "ona.dataset.init(\"%s\", \"%s\");"
                                      username temp-token))))
(facts "About show"
       (show account dataset-id) => (every-checker
                                      (contains "Loading Data...")
                                      (contains (format "ona.dataview.base.init(\"%s\",\"%s\",\"%s\");\n"
                                                        dataset-id username temp-token "owner"))))
