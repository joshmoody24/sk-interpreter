(ns interpreter
  (:gen-class)
  (:require
   [parser] [clojure.string :as string]))

;; --- Pretty Printing ---

(defn pretty-print [expr]
  (cond
    (= (:type expr) :combinator) (:value expr)
    (= (:type expr) :placeholder) (:name expr)
    (= (:type expr) :application)
    (str "(" (string/join " " (map pretty-print (:expressions expr))) ")")
    (= (:type expr) :let)
    (str "(let " (:name (:placeholder expr)) " = " (pretty-print (:value expr))
         " in " (pretty-print (:body expr)) ")")
    :else (str expr)))

(defn evaluate [expr]
  (throw (Exception. "Not implemented")))

;; --- Main Execution ---

(defn run []
  (if (empty? *command-line-args*)
    (println "Usage: clojure -m interpreter <filename>")
    (let [filename (first *command-line-args*)
          input (slurp filename)
          parsed (parser/parse input)]
      (if (:success parsed)
        (do
          (println "Parsed definitions: " (pretty-print (get-in parsed [:result :definitions])))
          (println "Parsed expression: " (pretty-print (get-in parsed [:result :expression])))
          (println "Evaluated result: " (pretty-print (evaluate (:result parsed)))))
        (println "Parsing failed:" (:error parsed) (:remaining parsed))))))

(run)

