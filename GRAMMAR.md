The parser's grammar is as follows (see page 84):

```
expression      -> equality ;
equality        -> comparison ( ( "!=" | "==" ) comparison )* ;
comparison      -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term            -> factor ( ( "-" | "+" ) factor )* ;
factor          -> unary ( ( "/" | "*" ) unary )* ;
unary           -> ( "!" | "-" ) unary
                | primary ;
primary         -> NUMBER | STRING | "true" | "false" | "nil"                   
                | "(" expression ")" ;
```

Revised grammar from section `8.1`:

```
program         -> statement* EOF ;
statement       -> exprStmt
                | printStmt ;
exprStmt        -> expression ";" ;
printStmt       -> "print" expression ";" ;               
```

Revised grammar for statements that declare names:

```
program         -> declaration* EOF ;
declaration     -> varDecl 
                | statement ;
varDecl         -> "var" IDENTIFIER ( "=" expression )? ";" ;
statement       -> exprStmt
                | printStmt ;
```

For accessing a variable, we can use the `primary` expression:

```
primary     -> "true" | "false" | "nil"
            | NUMBER | STRING 
            | "(" expression ")"
            IDENTIFIER ;
```

Note that unlike the original primary, this includes IDENTIFIER.