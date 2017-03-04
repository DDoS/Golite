package ca.sapon.golite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;

import ca.sapon.golite.semantic.SemanticException;
import ca.sapon.golite.syntax.SyntaxException;
import ca.sapon.golite.syntax.print.PrinterException;
import golite.node.Start;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public final class App {
    private static final String PRETTY_EXTENSION = "pretty.go";
    private static final String SYMBOL_TABLE_EXTENSION = "symtab";
    private static final String TYPE_ANNOTATION_EXTENSION = "pptype.go";
    private String[] args;
    private File inputFile = null;
    private File outputFile = null;
    private Start ast = null;

    private App(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) {
        System.exit(new App(args).executeCommand());
    }

    private int executeCommand() {
        if (args.length <= 0) {
            System.err.println("Expected a command");
            return 1;
        }
        // Find the command to execute
        final String command = args[0];
        args = subArray(args, 1);
        switch (command) {
            case "parse":
                return parseCommand();
            case "print":
                return printCommand();
            case "typecheck":
                return typecheckCommand();
            default:
                System.err.println("Not a command: " + command);
                return 1;
        }
    }

    private int parseCommand() {
        // Get the input file
        if (args.length != 1) {
            System.err.println("Expected one input file, not " + args.length);
            return 1;
        }
        inputFile = new File(args[0]);
        // Create the source reader
        final Reader source;
        try {
            source = new BufferedReader(new FileReader(inputFile));
        } catch (FileNotFoundException exception) {
            System.err.println("Input file not found: " + exception.getMessage());
            return 1;
        }
        // Do the lexing, parsing and weeding
        try {
            ast = Golite.parse(source);
        } catch (SyntaxException exception) {
            System.err.println(exception.getMessage());
            return 1;
        } catch (IOException exception) {
            System.err.println("Error when reading source: " + exception.getMessage());
            return 1;
        }
        // Close the input file
        try {
            source.close();
        } catch (IOException exception) {
            System.err.println("Error when closing input file: " + exception.getMessage());
            return 1;
        }
        return 0;
    }

    private int printCommand() {
        // Parse the output file argument first
        int result = findOutputFile(true);
        if (result != 0) {
            return result;
        }
        // Then run the parse command
        result = parseCommand();
        if (result != 0) {
            return result;
        }
        // Derive the output file from the input one if missing
        defaultOutputFile(PRETTY_EXTENSION);
        // Create the output writer
        final Writer pretty;
        try {
            pretty = new FileWriter(outputFile);
        } catch (IOException exception) {
            System.err.println("Cannot create output file: " + exception.getMessage());
            return 1;
        }
        // Then do the pretty printing
        try {
            Golite.prettyPrint(ast, pretty);
        } catch (PrinterException exception) {
            System.err.println("Error when printing: " + exception.getMessage());
            return 1;
        }
        // Close the output file
        try {
            pretty.close();
        } catch (IOException exception) {
            System.err.println("Error when closing output file: " + exception.getMessage());
            return 1;
        }
        return 0;
    }

    private int typecheckCommand() {
        // Parse the output file argument first
        int result = findOutputFile(false);
        if (result != 0) {
            return result;
        }
        // Parse the arguments for the type check command
        final boolean dumpSymTab, dumpSymTabAll, ppType;
        try {
            final Options options = new Options();
            options.addOption("dumpsymtab", "Write the top of the symbol table at the end of each scope");
            options.addOption("dumpsymtaball", "Write the full symbol table at the end of each scope");
            options.addOption("pptype", "Add type annotations to the pretty-printed output");
            final CommandLine line = new DefaultParser().parse(options, args);
            dumpSymTab = line.hasOption("dumpsymtab");
            dumpSymTabAll = line.hasOption("dumpsymtaball");
            ppType = line.hasOption("pptype");
            // Set the arguments to the remaining ones
            args = line.getArgs();
        } catch (ParseException exception) {
            System.err.println("Invalid arguments: " + exception.getMessage());
            return 1;
        }
        // Then run the parse command
        result = parseCommand();
        if (result != 0) {
            return result;
        }
        // Derive the output file from the input one if missing
        defaultOutputFile(SYMBOL_TABLE_EXTENSION);
        // Do the type checking
        try {
            Golite.typeCheck(ast);
        } catch (SemanticException exception) {
            System.err.println(exception.getMessage());
            return 1;
        }
        // TODO: print the type output
        return 0;
    }

    private int findOutputFile(boolean lastOption) {
        try {
            final Options options = new Options();
            final Option outputFileOption = Option.builder("o").longOpt("output").hasArg().argName("file")
                    .desc("The output file").type(File.class).build();
            options.addOption(outputFileOption);
            final CommandLine line = new DefaultParser().parse(options, args, !lastOption);
            outputFile = (File) line.getParsedOptionValue("output");
            // Set the arguments to the remaining ones
            args = line.getArgs();
        } catch (ParseException exception) {
            System.err.println("Invalid arguments: " + exception.getMessage());
            return 1;
        }
        return 0;
    }

    private void defaultOutputFile(String extension) {
        if (outputFile != null) {
            return;
        }
        final String inputName = inputFile.getName();
        final int index = inputName.lastIndexOf('.');
        final String outputName;
        if (index < 0) {
            outputName = inputName + '.' + extension;
        } else {
            outputName = inputName.substring(0, index + 1) + extension;
        }
        outputFile = new File(inputFile.getParent(), outputName);
    }

    private static <T> T[] subArray(T[] array, int start) {
        int length = Array.getLength(array);
        final int newLength = length - start;
        @SuppressWarnings("unchecked")
        final T[] sub = (T[]) Array.newInstance(array.getClass().getComponentType(), newLength);
        System.arraycopy(array, start, sub, 0, newLength);
        return sub;
    }
}
