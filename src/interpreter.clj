(ns interpreter
  (:gen-class)
  (:require
   [parser]
   [clojure.string :as string]))

;; --- Helper Function for applying functions, unwrapping metadata maps ---
(defn call-fn [f arg]
  (if (and (map? f) (contains? f :function))
    ((:function f) arg)
    (f arg)))

;; --- Pretty Printing ---
(defn pretty-print [ast]
  (cond
    (vector? ast)
    (string/join "\n" (map pretty-print ast))

    (map? ast)
    (cond
      (and (contains? ast :name) (contains? ast :definition) (contains? ast :function))
      (str (:name ast) " = " (:definition ast))

      :else
      (case (:type ast)
        :combinator (:name ast)
        :derived-combinator (:name ast)
        :application-expression
        (str "(" (string/join " " (map pretty-print (:expressions ast))) ")")
        :definition
        (str (:name ast) " = " (pretty-print (:expression ast)) ";")
        (if (and (contains? ast :definitions)
                 (contains? ast :expression))
          (str (pretty-print (:definitions ast))
               "\n"
               (pretty-print (:expression ast)))
          (pr-str ast))))

    :else (str ast)))

;; --- Interpreter ---

(defn interpret-derived-combinator [combinator env]
  (println "Interpreting derived combinator" combinator)
  (if-let [value (get env (:name combinator))]
    value
    (throw (Exception. (str "Unknown derived combinator: " (:name combinator))))))

(defn interpret-combinator [combinator]
  (println "Interpreting combinator" combinator)
  (case (:name combinator)
    "S" {:name "S"
         :definition "S"
         :function (fn [x]
                     (fn [y]
                       (fn [z]
                         (call-fn (call-fn x z) (call-fn y z)))))}
    "K" {:name "K"
         :definition "K"
         :function (fn [x]
                     (fn [_] x))}
    (throw (Exception. (str "Unknown base combinator: " (:name combinator))))))

(declare interpret-primary-expression)

(defn interpret-application-expression [expr env]
  (println "Interpreting application expression" expr)
  (if (= (:type expr) :application-expression)
    (let [expressions (:expressions expr)
          head (interpret-primary-expression (first expressions) env)
          tail (map #(interpret-primary-expression % env) (rest expressions))]
      (reduce (fn [f arg]
                (call-fn f arg))
              head tail))
    (interpret-primary-expression expr env)))

(defn interpret-primary-expression [expr env]
  (println "Interpreting primary expression" expr)
  (cond
    (= (:type expr) :combinator)
    (interpret-combinator expr)

    (= (:type expr) :derived-combinator)
    (interpret-derived-combinator expr env)

    (= (:type expr) :application-expression)
    (interpret-application-expression expr env)

    :else
    (throw (Exception. (pr-str "Expected primary expression, got: " expr)))))

(defn interpret-definition [definition env]
  (println "Interpreting definition" definition)
  (let [result (interpret-application-expression (:expression definition) env)]
    {:name       (:name definition)
     :definition (pretty-print (:expression definition))
     :function   (if (and (map? result) (contains? result :function))
                   (:function result)
                   result)}))

(defn interpret-definitions [definitions]
  (println "Interpreting definitions" definitions)
  (reduce (fn [acc definition]
            (assoc acc (:name definition) (interpret-definition definition acc)))
          {} definitions))

(defn interpret-program [parsed]
  (println "Interpreting program" parsed)
  (let [env (interpret-definitions (:definitions parsed))]
    (interpret-application-expression (:expression parsed) env)))

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
            (println "Evaluated result" (pretty-print eval-result))
            (catch Exception e
              (println "Error" e "Evaluated result (as combinatory form):" (pretty-print eval-result)))))
        (println "Parsing failed:" (:error parsed) (:remaining parsed))))))

(run)

