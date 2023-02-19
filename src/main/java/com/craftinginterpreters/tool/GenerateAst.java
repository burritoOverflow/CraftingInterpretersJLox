package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_at <output_directory>\n");
            System.exit(64);
        }

        final String outputDir = args[0];
        System.out.printf("Generating AST in directory: %s\n", outputDir);

        defineAst(outputDir, "Expr", Arrays.asList(
                "Binary   : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Unary    : Token operator, Expr right"
        ));
    }

    private static void defineAst(String outputDir, String basename, List<String> types) throws IOException {
        final String path = String.format("%s/%s.java", outputDir, basename);
        final PrintWriter printWriter = new PrintWriter(path, "UTF-8");

        printWriter.println("package com.craftinginterpreters.lox;");
        printWriter.println();
        printWriter.println("import java.util.List;");
        printWriter.println();
        printWriter.println(String.format("abstract class %s {", basename));

        // generate AST classes.
        for (String type : types) {
            final String className = type.split(":")[0].trim();
            final String fields = type.split(":")[1].trim();
            defineType(printWriter, basename, className, fields);
        }

        printWriter.println("}");
        printWriter.close();
    }

    private static void defineType(PrintWriter writer, String basename, String className, String fieldList) {
        writer.println(String.format("  static class %s extends %s {", className, basename));

        // store parameters in fields
        final String[] fields = fieldList.split(", ");

        // Fields first
        for (final String field : fields) {
            writer.println(String.format("      final %s;", field));
        }
        writer.println();

        // constructor
        writer.println(String.format("    %s(%s) {", className, fieldList));
        for (final String field : fields) {
            final String name = field.split(" ")[1];
            writer.println(String.format("       this.%s = %s;", name, name));
        }
        writer.println("      }");

        // end constructor
        writer.println();
        writer.println("    }");
        writer.println();
    }
}
