(ns interpreter
  (:gen-class))

;; Define the SK combinators
(defn S [x y z]
  ((x z) (y z)))

(defn K [x y]
  x)

(defn eval-sk [expr]
  "Evaluates an SK combinator expression."
  (cond
    (list? expr) (let [[op & args] expr]
                   (case op
                     'S (apply S args)
                     'K (apply K args)
                     expr))
    :else expr))

(defn -main []
  (println "Enter an SK combinator expression:")
  (let [expr (read)]
    (println "Result:" (eval-sk expr))))

