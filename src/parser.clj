(ns parser
  (:gen-class))

;; --- Tokenization Phase ---

(def reserved-tokens #{"let" "in" "S" "K" "(" ")" "="})

(defn tokenize [input]
  (->> (re-seq #"[A-Za-z0-9]+|[\(\)=]" input)
       (map (fn [tok]
              {:type (if (contains? reserved-tokens tok)
                       :keyword
                       :identifier)
               :value tok}))
       vec))

;; --- Basic Parser Combinators ---

(defn seq-p [& parsers]
  (fn [tokens]
    (reduce (fn [[results remaining] parser]
              (if (nil? remaining)
                (reduced nil)
                (let [[result new-remaining] (parser remaining)]
                  (if (nil? result)
                    (reduced nil)
                    [(conj results result) new-remaining]))))
            [[] tokens]
            parsers)))

(defn choice-p [& parsers]
  (fn [tokens]
    (some #(when-let [res (% tokens)] res) parsers)))

(defn many-p [parser]
  (fn [tokens]
    (loop [remaining tokens, acc []]
      (if-let [[result new-remaining] (parser remaining)]
        (recur new-remaining (conj acc result))
        [acc remaining]))))

(defn match-token-p [expected]
  (fn [tokens]
    (when (seq tokens)
      (let [token (first tokens)]
        (if (= (:value token) expected)
          [token (rest tokens)]
          nil)))))

;; --- Language Parsers ---

;; avoid mutual recursion warning
(declare expression-p)

(defn sk-combinator-p [tokens]
  (when (seq tokens)
    (let [token (first tokens)]
      (if (and (= (:type token) :keyword)
               (#{"S" "K"} (:value token)))
        [{:type :combinator, :value (:value token)} (rest tokens)]
        nil))))

(defn placeholder-p [tokens]
  (when (seq tokens)
    (let [token (first tokens)]
      (if (= (:type token) :identifier)
        [{:type :placeholder, :name (:value token)} (rest tokens)]
        nil))))

(defn parenthetical-p [tokens]
  ;; unwrap the parenthetical expression by returning just the inner result.
  (when-let [[[_ expr _] rest-tokens]
             ((seq-p (match-token-p "(")
                     expression-p
                     (match-token-p ")")) tokens)]
    [expr rest-tokens]))

(defn atomic-expression-p [tokens]
  ((choice-p sk-combinator-p placeholder-p parenthetical-p) tokens))

(defn application-p [tokens]
  (let [[atoms remaining] ((many-p atomic-expression-p) tokens)]
    (if (> (count atoms) 1)
      [{:type :application, :expressions atoms} remaining]
      [(first atoms) remaining])))

(defn let-expression-p [tokens]
  (when-let [[[_ placeholder _ value _ body] rest-tokens]
             ((seq-p (match-token-p "let")
                     placeholder-p
                     (match-token-p "=")
                     expression-p
                     (match-token-p "in")
                     expression-p) tokens)]
    [{:type :let
      :placeholder placeholder
      :value value
      :body body}
     rest-tokens]))

(defn expression-p [tokens]
  ((choice-p let-expression-p application-p) tokens))

;; --- Running the Parser ---

(defn parse [input]
  (let [tokens (tokenize input)]
    (if (empty? tokens)
      {:success false, :error "Unconsumed input", :result nil, :remaining []}
      (let [[result remaining] (expression-p tokens)]
        (if (seq remaining)
          {:success false, :error "Unconsumed input", :result result, :remaining remaining}
          {:success true, :result, result})))))

