The parser's grammar is as follows (see page 84, 199):

```
expression      -> assignment ;
assignment      -> IDENTIFIER "=" assignment 
                | logic_or ;
logic_or        -> logic_and ( "or" logic_and )* ;
logic_and       -> equality ( "and" equality )* ;
equality        -> comparison ( ( "!=" | "==" ) comparison )* ;
comparison      -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term            -> factor ( ( "-" | "+" ) factor )* ;
factor          -> unary ( ( "/" | "*" ) unary )* ;
unary           -> ( "!" | "-" ) unary
                | call ;
call            -> primary ( "(" arguments? ")"  | "." IDENTIFIER )* ;
primary         -> NUMBER | STRING | "true" | "false" | "nil"                   
                | "(" expression ")" ;
                IDENTIFIER ;
```

Revised statement grammar from section `8.1` and `9.1`.

```
program         -> statement* EOF ;
statement       -> exprStmt
                | forStmt
                | ifStmt
                | printStmt
                | returnStmt
                | whileStmt
                | block ;
exprStmt        -> expression ";" ;
forStmt         -> "for" "(" ( varDecl | exprStmt | ";" )
                expression? ";"
                expression? ")" statement ;
ifStmt          -> "if" "(" expression ")" statement ;
                ( "else" statement )? ;
printStmt       -> "print" expression ";" ;
returnStmt      -> "return expression? ";" ;
whileStmt       -> "while" "(" expression ")" statement ;
block           -> "{" declaration* "}" ;
```

Revised grammar for declarations that declare classes, names, and functions:

```
program         -> declaration* EOF ;
declaration     -> classDecl 
                | funcDecl 
                | varDecl 
                | statement ;
classDecl       -> "class" IDENTIFIER "{" function* "}" ;
funcDecl        -> "fun" function ;
varDecl         -> "var" IDENTIFIER ( "=" expression )? ";" ;
```

Utility rules

```
function        -> IDENTIFIER "(" parameters? ")" block ;
parameters      -> IDENTIFIER ( "," IDENTIFIER )* ;
arguments       -> expression ( "," expression )* ;
```

For accessing a variable, we can use the `primary` expression:

```
primary     -> "true" | "false" | "nil"
            | NUMBER | STRING 
            | "(" expression ")"
            IDENTIFIER ;
```

Note that unlike the original primary, this includes IDENTIFIER.