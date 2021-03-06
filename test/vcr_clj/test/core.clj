(ns vcr-clj.test.core
  (:refer-clojure :exclude [get])
  (:require [bond.james :refer [calls with-spy]]
            [clj-http.client :as client]
            [clojure.test :refer :all]
            [vcr-clj.core :refer [with-cassette]]
            [vcr-clj.test.helpers :as help]))

(use-fixtures :each help/delete-cassettes-after-test)

;; some test fns
(defn plus [a b] (+ a b))
(defn increment [x] (inc x))

(deftest basic-test
  (with-spy [plus]
    (is (empty? (calls plus)))
    (with-cassette :bang [{:var #'plus}]
      (is (= 5 (plus 2 3)))
      (is (= 1 (count (calls plus)))))
    ;; Check that it replays correctly without calling the original
    (with-cassette :bang [{:var #'plus}]
      (is (= 5 (plus 2 3)))
      (is (= 1 (count (calls plus)))))
    ;; Check that different calls throw an exception
    (with-cassette :bang [{:var #'plus}]
      (is (thrown? clojure.lang.ExceptionInfo
            (plus 3 4))))))

(deftest two-vars-test
  (with-spy [plus increment]
    (is (empty? (calls plus)))
    (is (empty? (calls increment)))

    (with-cassette :baz [{:var #'plus} {:var #'increment}]
      (is (= 42 (increment 41)))
      (is (= 79 (plus 42 37)))
      (is (= 42 (increment 41))))

    (is (= 2 (count (calls increment))))
    (is (= 1 (count (calls plus))))

    (with-cassette :baz [{:var #'plus} {:var #'increment}]
      (is (= 42 (increment 41)))
      (is (= 79 (plus 42 37)))
      (is (= 42 (increment 41)))
      (is (= 2 (count (calls increment))))
      (is (= 1 (count (calls plus)))))

    (is (= 2 (count (calls increment))))
    (is (= 1 (count (calls plus))))))

(deftest recordable?-test
  (with-spy [plus]
    (with-cassette :breezy [{:var #'plus
                             :recordable? (fn [a _] (even? a))}]
      (is (= 7 (plus 3 4)))
      (is (= 9 (plus 4 5))))

    (is (= 2 (count (calls plus))))

    (with-cassette :breezy [{:var #'plus
                             :recordable? (fn [a _] (even? a))}]
      (is (= 7 (plus 3 4)))
      (is (= 9 (plus 4 5))))

    ;; One of the two calls went through
    (is (= 3 (count (calls plus))))))

(deftest arg-key-fn-test
  (with-cassette :blammo [{:var #'increment
                           :arg-key-fn #(mod % 2)}]
    (is (= 42 (increment 41))))

  (with-cassette :blammo [{:var #'increment
                           :arg-key-fn #(mod % 2)}]
    (is (= 42 (increment 29)))))
