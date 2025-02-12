(ns parser
  (:gen-class))

;; --- Tokenization Phase ---

(def reserved-tokens #{"let" "in" "S" "K" "(" ")" "=" ";"})

(defn tokenize [input]
  (->> (re-seq #"[A-Za-z0-9]+|[\(\);=]" input)
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
(declare application-p)

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
                     application-p
                     (match-token-p ")")) tokens)]
    [expr rest-tokens]))

(defn atomic-expression-p [tokens]
  ((choice-p sk-combinator-p placeholder-p parenthetical-p) tokens))

(defn application-p [tokens]
  (let [[atoms remaining] ((many-p atomic-expression-p) tokens)]
    (if (> (count atoms) 1)
      [{:type :application, :expressions atoms} remaining]
      [(first atoms) remaining])))

(defn definition-expression-p [tokens]
  (when-let [[[placeholder _ expr _] rest-tokens]
             ((seq-p
               placeholder-p
               (match-token-p "=")
               application-p
               (match-token-p ";")) tokens)]
    [{:type :definition
      :placeholder placeholder
      :value expr}
     rest-tokens]))

(defn program-p [tokens]
  (when-let [[[defs expr] rest-tokens]
             ((seq-p (many-p definition-expression-p) application-p) tokens)]
    [{:definitions defs
      :expression  expr}
     rest-tokens]))

;; --- Running the Parser ---

(defn parse [input]
  (let [tokens (tokenize input)]
    (if (empty? tokens)
      {:success false, :error "Unconsumed input", :result nil, :remaining []}
      (if-let [[result remaining] (program-p tokens)]
        (if (seq remaining)
          {:success false, :error "Unconsumed input", :result result, :remaining remaining}
          {:success true, :result result})
        {:success false, :error "Parsing failed", :result nil}))))

