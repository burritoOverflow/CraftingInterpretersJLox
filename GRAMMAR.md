The parser's grammar is as follows (see page 84):

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
                | primary ;
primary         -> NUMBER | STRING | "true" | "false" | "nil"                   
                | "(" expression ")" ;
                IDENTIFIER ;
```

Revised grammar from section `8.1` and `9.1`.

```
program         -> statement* EOF ;
statement       -> exprStmt
                | forStmt
                | ifStmt
                | printStmt
                | whileStmt
                | block ;
exprStmt        -> expression ";" ;
forStmt         -> "for" "(" ( varDecl | exprStmt | ";" )
                expression? ";"
                expression? ")" statement ;
ifStmt          -> "if" "(" expression ")" statement ;
                ( "else" statement )? ;
printStmt       -> "print" expression ";" ;               
whileStmt       -> "while" "(" expression ")" statement ;
block           -> "{" declaration* "}" ;
```

Revised grammar for statements that declare names:

```
program         -> declaration* EOF ;
declaration     -> varDecl 
                | statement ;
varDecl         -> "var" IDENTIFIER ( "=" expression )? ";" ;
statement       -> exprStmt
                | printStmt
                | block ;
block           -> "{" declaration* "}" ;
```

For accessing a variable, we can use the `primary` expression:

```
primary     -> "true" | "false" | "nil"
            | NUMBER | STRING 
            | "(" expression ")"
            IDENTIFIER ;
```

Note that unlike the original primary, this includes IDENTIFIER.