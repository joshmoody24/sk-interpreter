# sk-interpreter

This is a simple interpreter for the simplest possible general-purpose programming language. It is based on the SK combinator calculus, which is a minimalist Turing-complete model of computation.

## Syntax

```
Program         ::= Expression

Expression      ::= Application | AtomicExpression | LetExpression

Application     ::= Application AtomicExpression | AtomicExpression

AtomicExpression ::= Combinator
                   | Placeholder
                   | "(" Expression ")"

LetExpression   ::= "let" Placeholder "=" Expression "in" Expression

Combinator      ::= "S" | "K"

Placeholder     ::= [a-zA-Z][a-zA-Z0-9]* - ("S" | "K")

```
