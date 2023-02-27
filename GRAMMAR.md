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

Grammar from section `8.1`:

```
program         -> statement* EOF ;
statement       -> exprStmt
                | printStmt ;
exprStmt        -> expression ";" ;
printStmt       -> "print" expression ";" ;               
```