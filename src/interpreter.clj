(ns interpreter
  (:gen-class)
  (:require
   [parser]
   [clojure.string :as string]))

;; --- Pretty Printing (for AST only) ---
(defn pretty-print [ast]
  (cond
    (vector? ast) (string/join "\n" (map pretty-print ast))
    (map? ast)
    (case (:type ast)
      :combinator (:name ast)
      :derived-combinator (:name ast)
      :application-expression (str "(" (string/join " " (map pretty-print (:expressions ast))) ")")
      :definition (str (:name ast) " = " (pretty-print (:expression ast)) ";")
      (if (and (contains? ast :definitions) (contains? ast :expression))
        (str (pretty-print (:definitions ast)) "\n" (pretty-print (:expression ast)))
        (pr-str ast)))
    :else (str ast)))

;; --- Interpreter ---

(defn interpret-combinator [combinator]
  (case (:name combinator)
    "S" (fn [x]
          (fn [y]
            (fn [z]
              ((x z) (y z)))))
    "K" (fn [x]
          (fn [_] x))
    (throw (Exception. (str "Unknown base combinator: " (:name combinator))))))

(defn interpret-derived-combinator [combinator env]
  (if-let [value (get env (:name combinator))]
    value
    (throw (Exception. (str "Unknown derived combinator: " (:name combinator))))))

(declare interpret-primary-expression)

(defn interpret-application-expression [expr env]
  (if (= (:type expr) :application-expression)
    (let [expressions (:expressions expr)
          head (interpret-primary-expression (first expressions) env)
          tail (map #(interpret-primary-expression % env) (rest expressions))]
      (reduce (fn [f arg] (f arg)) head tail))
    (interpret-primary-expression expr env)))

(defn interpret-primary-expression [expr env]
  (cond
    (= (:type expr) :combinator) (interpret-combinator expr)
    (= (:type expr) :derived-combinator) (interpret-derived-combinator expr env)
    (= (:type expr) :application-expression) (interpret-application-expression expr env)
    :else (throw (Exception. (pr-str "Expected primary expression, got: " expr)))))

(defn interpret-definition [definition env]
  (interpret-application-expression (:expression definition) env))

(defn interpret-definitions [definitions]
  (reduce (fn [acc definition]
            (assoc acc (:name definition) (interpret-definition definition acc)))
          {} definitions))

(defn interpret-program [parsed]
  (let [env (interpret-definitions (:definitions parsed))]
    (interpret-application-expression (:expression parsed) env)))

;; --- Church Numeral Testing ---
(defn apply-church-numeral [fnc]
  (try
    (let [test-fn (fn [x] (inc x))
          result  ((fnc test-fn) 0)]
      (if (integer? result)
        result
        nil))
    (catch Exception _ nil)))

;; --- Main Execution ---
(defn run []
  (if (empty? *command-line-args*)
    (println "Usage: clojure -m interpreter <filename>")
    (let [filename (first *command-line-args*)
          input (slurp filename)
          parsed (parser/parse input)]
      (if (:success parsed)
        (let [prog        (:result parsed)
              eval-result (interpret-program prog)]
          (println "Parsed definitions:\n" (pretty-print (:definitions prog)))
          (println "Parsed expression:" (pretty-print (:expression prog)))
          (try
            (println "Evaluated result (combinatory form):"
                     (if (fn? eval-result)
                       "<Function Output>"
                       (pretty-print eval-result)))
            (let [num-result (apply-church-numeral eval-result)]
              (if (number? num-result)
                (println "Evaluated result (as Church numeral):" num-result)
                (println "Could not interpret as a Church numeral.")))
            (catch Exception e
              (println "Error:" (.getMessage e)))))
        (println "Parsing failed:" (:error parsed) (:remaining parsed))))))

(run)
