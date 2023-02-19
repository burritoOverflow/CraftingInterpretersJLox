load("@rules_java//java:defs.bzl", "java_binary")

java_binary(
    name = "GenerateAst",
    srcs = ["src/main/java/com/craftinginterpreters/tool/GenerateAst.java"],
    main_class = "com.craftinginterpreters.tool.GenerateAst"
)

java_binary(
    name = "Lox",
    srcs = glob(["src/main/java/com/craftinginterpreters/**/*.java"]),
    main_class = "com.craftinginterpreters.lox.Lox",
    deps =[":GenerateAst"]
)

java_binary(
    name = "AstPrinter",
    srcs = glob(["src/main/java/com/craftinginterpreters/**/*.java"]),
    main_class = "com.craftinginterpreters.lox.AstPrinter",
    deps =[":GenerateAst"]
)
