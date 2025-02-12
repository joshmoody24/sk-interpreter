# sk-interpreter

This is a simple interpreter for the simplest possible general-purpose programming language. It is based on the SK combinator calculus, which is a minimalist Turing-complete model of computation.

## Syntax

```
Program               ::= DefinitionStatements Expression

DefinitionStatements  ::== DefinitionStatement DefinitionStatements

DefinitionStatement   ::= DerivedCombinator "=" Expression ";"

Application           ::= Application AtomicExpression | AtomicExpression

AtomicExpression      ::= Combinator | "(" Expression ")"

Combinator            ::= BaseCombinator | DerivedCombinator

BaseCombinator        ::= "S" | "K"

DerivedCombinator     ::= [a-zA-Z][a-zA-Z0-9]* - ("S" | "K")

```

## Example Combinators

The identity function (extremely useful):

```
I = S K K;
```

Some commonly accepted definitions:

```
TRUE = K;
FALSE = K I;
```

### Useful combinators from To Mock a Mockingbird:

| Combinator | Bird Name   | Definition        |
| ---------- | ----------- | ----------------- |
| **B**      | Bluebird    | `S (K S) K`       |
| **C**      | Cardinal    | `S (B B S) (K K)` |
| **W**      | Warbler     | `S I I`           |
| **T**      | Thrush      | `C I`             |
| **J**      | Jaybird     | `S B B`           |
| **M**      | Mockingbird | `S I I`           |
