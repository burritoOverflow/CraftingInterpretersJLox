package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class GenerateAst {
    private static final String GENERATED_CODE_MESSAGE = "// This file is generated by `GenerateAst`; DO NOT EDIT.";
    private static final String PACKAGE_NAME = "package com.craftinginterpreters.lox;";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_at <output_directory>\n");
            System.exit(64);
        }

        final String outputDir = args[0];
        System.out.printf("Generating AST in directory: %s\n", outputDir);

        // generate the `Expr` class with the corresponding derived classes
        defineAst(outputDir, "Expr", Arrays.asList(
                "Binary   : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Variable : Token name",
                "Unary    : Token operator, Expr right"
        ));

        // define the 'Stmt' class and the corresponding derived classes
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Expression  : Expr expression",
                "Print       : Expr expression",
                "Var         : Token name, Expr initializer"
        ));
    }

    /**
     * Generate a "generated" at string
     *
     * @param filename the name of the file without a file extension
     * @return a generated string with the filename and time/date generated
     */
    private static String createGeneratedString(String filename) {
        final Date currentDate = new Date();
        final Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final String dateStr = formatter.format(currentDate);
        return String.format("// %s.java - generated on %s", filename, dateStr);
    }

    /**
     * Generate the Source file for the abstract class and the derived class implementations
     *
     * @param outputDir path to generate the source file for the given BaseName
     * @param basename  the base class
     * @param types     List of types for derived classes
     * @throws IOException
     */
    private static void defineAst(String outputDir, String basename, List<String> types) throws IOException {
        System.out.printf("Generating %s.java in directory %s%n", basename, outputDir);

        // outfile name is the Base type
        final String path = String.format("%s/%s.java", outputDir, basename);
        final PrintWriter printWriter = new PrintWriter(path, "UTF-8");

        printWriter.println(PACKAGE_NAME);
        printWriter.println();
        printWriter.println(GENERATED_CODE_MESSAGE);
        printWriter.println(createGeneratedString(basename));
        printWriter.println();
        printWriter.println("import java.util.List;");
        printWriter.println();
        printWriter.println(String.format("abstract class %s {", basename));

        // generate the Visitor interface and the derived classes inside of the base class
        defineVisitor(printWriter, basename, types);

        // generate AST classes.
        for (String type : types) {
            final String className = type.split(":")[0].trim();
            final String fields = type.split(":")[1].trim();
            defineType(printWriter, basename, className, fields);
        }

        // base `accept` method
        printWriter.println("    abstract <R> R accept(Visitor<R> visitor);");
        printWriter.println("}");
        printWriter.close();
    }

    /**
     * Generate the code that defines the Visitor interface on the base class (used with the `Visitor` pattern)
     *
     * @param printWriter writer for the generated source file
     * @param basename    base class name, i.e `Expr`
     * @param types       the derived class types
     */
    private static void defineVisitor(PrintWriter printWriter, String basename, List<String> types) {
        printWriter.println("    interface Visitor<R> {");

        for (final String type : types) {
            final String typeName = type.split(":")[0].trim();
            printWriter.println(String.format("      R visit%s%s(%s %s);", typeName, basename, typeName, basename.toLowerCase()));
        }

        printWriter.println("   }");
    }

    /**
     * Generate code for the class `className` that implements the `accept` method for the `Visitor` pattern.
     *
     * @param writer    PrintWriter for writing the source file
     * @param basename  the base class Type name
     * @param className this derived class
     * @param fieldList the list of members/fields for this type
     */
    private static void defineType(PrintWriter writer, String basename, String className, String fieldList) {
        writer.println(String.format("  static class %s extends %s {", className, basename));

        // implementation of the abstract `accept` method for each derived class
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println(String.format("      return visitor.visit%s%s(this);", className, basename));
        writer.println("    }");
        writer.println();

        // store parameters in fields
        final String[] fields = fieldList.split(", ");

        // Fields first
        for (final String field : fields) {
            writer.println(String.format("    final %s;", field));
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
        writer.println("    }");
        writer.println();
    }
}
