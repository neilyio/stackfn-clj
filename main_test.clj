(ns main-test
  (:require [clojure.test :as t]
            [main :as m]))


(t/deftest addition
  (t/testing "for real this time"
    (t/is (= 4 (+ 2 2)))
    (t/is (= 5 (m/add 1 4)))))


;; This will only run the tests inside "deftest" blocks.
;; The bare "is" functions will not run at all.
;; You can omit it and CIDER's tests will still run.
(t/run-tests)
