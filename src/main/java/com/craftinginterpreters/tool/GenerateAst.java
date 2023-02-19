package com.craftinginterpreters.tool;

import java.io.IOException;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_at <output_directory>\n");
            System.exit(64);
        }

        final String outputDir = args[0];
        System.out.printf("Generating AST in directory: %s\n", outputDir);
    }
}
