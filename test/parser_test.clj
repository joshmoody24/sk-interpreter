(ns parser-test
  (:require [clojure.test :refer :all]
            [parser :refer :all]))

(deftest test-tokenization
  (is (= (tokenize "S")
         [{:type :keyword, :value "S"}]))
  (is (= (tokenize "K")
         [{:type :keyword, :value "K"}]))
  (is (= (tokenize "x")
         [{:type :identifier, :value "x"}]))
  (is (= (tokenize "x = S; x K")
         [{:type :identifier, :value "x"}
          {:type :keyword, :value "="}
          {:type :keyword, :value "S"}
          {:type :keyword, :value ";"}
          {:type :identifier, :value "x"}
          {:type :keyword, :value "K"}]))
  (is (= (tokenize "(S K)")
         [{:type :keyword, :value "("}
          {:type :keyword, :value "S"}
          {:type :keyword, :value "K"}
          {:type :keyword, :value ")"}]))
  (is (= (tokenize "")
         [])))

(deftest test-parsing
  (is (= (parse "S")
         {:success true,
          :result {:definitions []
                   :expression {:type :combinator, :name "S"}}}))

  (is (= (parse "K")
         {:success true,
          :result {:definitions []
                   :expression {:type :combinator, :name "K"}}}))

  (is (= (parse "x")
         {:success true,
          :result {:definitions []
                   :expression {:type :derived-combinator, :name "x"}}}))

  (is (= (parse "S K x")
         {:success true,
          :result {:definitions []
                   :expression {:type :application-expression,
                                :expressions [{:type :combinator, :name "S"}
                                              {:type :combinator, :name "K"}
                                              {:type :derived-combinator, :name "x"}]}}}))

  (is (= (parse "( S K ) x")
         {:success true,
          :result {:definitions []
                   :expression {:type :application-expression,
                                :expressions [{:type :application-expression,
                                               :expressions [{:type :combinator, :name "S"}
                                                             {:type :combinator, :name "K"}]}
                                              {:type :derived-combinator, :name "x"}]}}}))

  (is (= (parse "x = S; x K")
         {:success true,
          :result {:definitions [{:type :definition,
                                  :name "x",
                                  :expression {:type :combinator, :name "S"}}],
                   :expression {:type :application-expression,
                                :expressions [{:type :derived-combinator, :name "x"}
                                              {:type :combinator, :name "K"}]}}}))

  (is (= (parse "x = (S K); x x")
         {:success true,
          :result {:definitions [{:type :definition,
                                  :name "x",
                                  :expression {:type :application-expression,
                                               :expressions [{:type :combinator, :name "S"}
                                                             {:type :combinator, :name "K"}]}}],
                   :expression {:type :application-expression,
                                :expressions [{:type :derived-combinator, :name "x"}
                                              {:type :derived-combinator, :name "x"}]}}}))

  ;; Edge cases:
  (is (= (parse "")
         {:success false, :error "Unconsumed input", :result nil, :remaining []}))
  (let [parsed (parse "x = y;")]
    (is (= (select-keys parsed [:success :error])
           {:success false, :error "Parsing failed"})))
  (let [parsed (parse "x = (S K")]
    (is (= (select-keys parsed [:success :error])
           {:success false, :error "Unconsumed input"}))))

(run-tests)

