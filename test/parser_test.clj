(ns parser-test
  (:require [clojure.test :refer :all]
            [parser :refer :all]))

(deftest test-tokenization
  (is (= (tokenize "S") [{:type :keyword, :value "S"}]))
  (is (= (tokenize "K") [{:type :keyword, :value "K"}]))
  (is (= (tokenize "x") [{:type :identifier, :value "x"}]))
  (is (= (tokenize "let x = S in x K")
         [{:type :keyword, :value "let"}
          {:type :identifier, :value "x"}
          {:type :keyword, :value "="}
          {:type :keyword, :value "S"}
          {:type :keyword, :value "in"}
          {:type :identifier, :value "x"}
          {:type :keyword, :value "K"}]))
  (is (= (tokenize "(S K)")
         [{:type :keyword, :value "("}
          {:type :keyword, :value "S"}
          {:type :keyword, :value "K"}
          {:type :keyword, :value ")"}]))
  (is (= (tokenize "") [])))

(deftest test-parsing
  (is (= (parse "S") {:success true, :result {:type :combinator, :value "S"}}))
  (is (= (parse "K") {:success true, :result {:type :combinator, :value "K"}}))
  (is (= (parse "x") {:success true, :result {:type :placeholder, :name "x"}}))
  (is (= (parse "S K x")
         {:success true,
          :result {:type :application,
                   :expressions [{:type :combinator, :value "S"}
                                 {:type :combinator, :value "K"}
                                 {:type :placeholder, :name "x"}]}}))
  (is (= (parse "( S K ) x")
         {:success true,
          :result {:type :application,
                   :expressions [{:type :application,
                                  :expressions [{:type :combinator, :value "S"}
                                                {:type :combinator, :value "K"}]}
                                 {:type :placeholder, :name "x"}]}}))
  (is (= (parse "let x = S in x K")
         {:success true,
          :result {:type :let,
                   :placeholder {:type :placeholder, :name "x"},
                   :value {:type :combinator, :value "S"},
                   :body {:type :application,
                          :expressions [{:type :placeholder, :name "x"}
                                        {:type :combinator, :value "K"}]}}}))
  (is (= (parse "let x = (S K) in x x")
         {:success true,
          :result {:type :let,
                   :placeholder {:type :placeholder, :name "x"},
                   :value {:type :application,
                           :expressions [{:type :combinator, :value "S"}
                                         {:type :combinator, :value "K"}]},
                   :body {:type :application,
                          :expressions [{:type :placeholder, :name "x"}
                                        {:type :placeholder, :name "x"}]}}}))

  ;; Edge cases
  (is (= (parse "") {:success false, :error "Unconsumed input", :result nil, :remaining []}))

  (let [parsed (parse "let x = y in")]
    (is (= (select-keys parsed [:success :error :result])
           {:success false, :error "Unconsumed input", :result nil})))

  (let [parsed (parse "let x = (S K")]
    (is (= (select-keys parsed [:success :error :result])
           {:success false, :error "Unconsumed input", :result nil}))))

(run-tests)
