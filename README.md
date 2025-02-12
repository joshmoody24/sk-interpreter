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

## Example Program

```
I = S K K;
K I I
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

## Planned Features

### Type Hints

```
B x y z -> x (y z) = S (K S) K;
```

### Implementation Hints

This is arguably cheating, but it's a useful feature nonetheless.

```
B x y z -> x (y z);
```

### Comments

```
# This is a comment
```

### Namespaced Imports

```
import foo.bar.baz as baz;
```
