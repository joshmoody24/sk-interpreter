# sk-interpreter

This is a simple interpreter for the simplest possible general-purpose programming language. It is based on the SK combinator calculus, which is a minimalist Turing-complete model of computation.

## Syntax

```
Program         ::= DefinitionStatements Expression

DefinitionStatements ::== DefinitionStatement DefinitionStatements

DefinitionStatement ::= Placeholder "=" Expression ";"

Application     ::= Application AtomicExpression | AtomicExpression

AtomicExpression ::= Combinator
                   | Placeholder
                   | "(" Expression ")"

Combinator      ::= "S" | "K"

Placeholder     ::= [a-zA-Z][a-zA-Z0-9]* - ("S" | "K")

```

## Examples

This defines the identity function, which is extremely useful:

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
| **J**      | Jay         | `S B B`           |
| **M**      | Mockingbird | `S I I`           |
