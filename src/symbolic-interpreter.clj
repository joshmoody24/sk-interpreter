
(ns interpreter
  (:gen-class)
  (:require
   [parser]
   [clojure.string :as string]))

;; --- Pretty Printing ---
(defn pretty-print [ast]
  (cond
    (vector? ast) (string/join "\n" (map pretty-print ast))
    (map? ast)
    (case (:type ast)
      :combinator (:name ast)
      :derived-combinator (:name ast)
      :application-expression (str "(" (string/join " " (map pretty-print (:expressions ast))) ")")
      :definition (str (:name ast) " = " (pretty-print (:expression ast)) ";")
      (if (and (contains? ast :definitions)
               (contains? ast :expression))
        (str (pretty-print (:definitions ast))
             "\n"
             (pretty-print (:expression ast)))
        (pr-str ast)))
    :else (str ast)))

;; --- Expand Derived Definitions ---
;; Build an environment mapping definition names to their AST bodies.
(defn build-definitions-env [definitions]
  (reduce (fn [env def]
            (assoc env (:name def) (:expression def)))
          {} definitions))

;; Recursively expand derived combinators using the definitions environment.
(defn expand-node [node env]
  (cond
    ;; If it's a derived combinator found in our env, inline its definition.
    (and (map? node) (= (:type node) :derived-combinator))
    (if (contains? env (:name node))
      (expand-node (get env (:name node)) env)
      node)
    ;; For application nodes, expand each subexpression.
    (and (map? node) (= (:type node) :application-expression))
    (assoc node :expressions (vec (map #(expand-node % env) (:expressions node))))
    :else node))

;; --- Symbolic Reduction Helpers ---
;; Flatten nested application expressions into a single vector.
(defn flatten-app [ast]
  (if (and (map? ast) (= (:type ast) :application-expression))
    (reduce (fn [acc sub]
              (if (and (map? sub) (= (:type sub) :application-expression))
                (into acc (flatten-app sub))
                (conj acc sub)))
            [] (:expressions ast))
    [ast]))

(defn make-app [terms]
  {:type :application-expression
   :expressions terms})

;; Predicates for S and K (either as base combinators or already expanded derived ones).
(defn is-S? [ast]
  (and (map? ast)
       (or (and (= (:type ast) :combinator) (= (:name ast) "S"))
           (and (= (:type ast) :derived-combinator) (= (:name ast) "S")))))

(defn is-K? [ast]
  (and (map? ast)
       (or (and (= (:type ast) :combinator) (= (:name ast) "K"))
           (and (= (:type ast) :derived-combinator) (= (:name ast) "K")))))

;; Perform one symbolic reduction step.
(defn symbolic-reduce-once [ast]
  (cond
    ;; Look for top-level application expressions.
    (and (map? ast) (= (:type ast) :application-expression))
    (let [terms (flatten-app ast)]
      (cond
        ;; K reduction: (K x) y  →  x
        (and (>= (count terms) 2) (is-K? (first terms)))
        (let [[_ x & rest] terms]
          (if (empty? rest)
            x
            (make-app (cons x rest))))
        ;; S reduction: ((S x) y) z  →  (x z) (y z)
        (and (>= (count terms) 3) (is-S? (first terms)))
        (let [[_ x y z & rest] terms
              new-term (make-app [(make-app [x z])
                                  (make-app [y z])])]
          (if (empty? rest)
            new-term
            (make-app (cons new-term rest))))
        ;; If no rule applies, try reducing the subexpressions.
        :else (update ast :expressions (fn [exprs]
                                         (vec (map symbolic-reduce-once exprs))))))
    (vector? ast) (vec (map symbolic-reduce-once ast))
    :else ast))

;; Fully reduce by iterating one-step reductions until no change.
(defn symbolic-fully-reduce [ast]
  (let [next (symbolic-reduce-once ast)]
    (if (= next ast)
      ast
      (recur next))))

;; --- Main Execution in Symbolic Mode ---
(defn run-symbolic []
  (if (empty? *command-line-args*)
    (println "Usage: clojure -m interpreter <filename>")
    (let [filename (first *command-line-args*)
          input (slurp filename)
          parsed (parser/parse input)]
      (if (:success parsed)
        (let [prog (:result parsed)
              definitions (:definitions prog)
              expr (:expression prog)
              env (build-definitions-env definitions)
              expanded (expand-node expr env)
              reduced (symbolic-fully-reduce expanded)]
          (println "Parsed definitions:")
          (println (pretty-print definitions))
          (println "\nOriginal AST:")
          (println (pretty-print expr))
          (println "\nExpanded AST (with definitions inlined):")
          (println (pretty-print expanded))
          (println "\nFully Reduced AST:")
          (println (pretty-print reduced)))
        (println "Parsing failed:" (:error parsed) (:remaining parsed))))))

(defn -main [& args]
  (run-symbolic))

(-main)

