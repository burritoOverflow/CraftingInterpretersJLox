### Crafting Interpreters

Java implementation of the `Lox` programming language from following along with
the `Crafting Interpreters` [book](https://craftinginterpreters.com/).

Prior to building the interpreter, build and run the `GenerateAst` JAR that generates required source files for the
Parser.

```shell
$ bazel build //:GenerateAst
```

And generate the source files in the same directory as the rest of the `Lox` package:

```shell
$ ./bazel-bin/GenerateAst src/main/java/com/craftinginterpreters/lox/
```

To build the interpreter with Bazel:

```shell
$ bazel build //:Lox
```

And run the interpreter:

```shell
$ ./bazel-bin/Lox
```
