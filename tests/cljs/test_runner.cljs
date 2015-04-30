(ns test-runner
  (:require
   [cljs.test :as test :refer-macros [run-tests] :refer [report]]))


(enable-console-print!)

(defmethod report [::test/default :summary] [m]
  (println "\nRan" (:test m) "tests containing"
           (+ (:pass m) (:fail m) (:error m)) "assertions.")
  (println (:fail m) "failures," (:error m) "errors.")
  (aset js/window "test-failures" (+ (:fail m) (:error m))))

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        (test/empty-env ::test/default)))
    0
    1))
