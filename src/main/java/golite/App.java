package golite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;

import golite.semantic.SemanticData;
import golite.semantic.check.TypeChecker;
import golite.semantic.check.TypeCheckerException;
import golite.syntax.SyntaxException;
import golite.syntax.print.PrinterException;
import golite.util.SourcePrinter;
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
        // Parse the arguments first
        final CommandLine line = parseOptions(outputFileOption());
        if (line == null) {
            return 1;
        }
        // Then run the parse command
        int result = parseCommand();
        if (result != 0) {
            return result;
        }
        // Get the output file (now that we have the input one)
        result = getOutputFile(line, PRETTY_EXTENSION);
        if (result != 0) {
            return result;
        }
        // Finally do the printing
        return printAST(null);
    }

    private int typecheckCommand() {
        // Parse the arguments first
        final CommandLine line = parseOptions(
                outputFileOption(),
                new Option("pptype", "Add type annotations to the pretty-printed output"),
                new Option("dumpsymtab", "Write the top of the symbol table at the end of each scope"),
                new Option("dumpsymtaball", "Write the full symbol table at the end of each scope")
        );
        if (line == null) {
            return 1;
        }
        // Then run the parse command
        int result = parseCommand();
        if (result != 0) {
            return result;
        }
        // Get the output file (now that we have the input one)
        result = getOutputFile(line, TYPE_ANNOTATION_EXTENSION);
        if (result != 0) {
            return result;
        }
        // Do the type checking
        final TypeChecker typeChecker = new TypeChecker();
        try {
            ast.apply(typeChecker);
            result = 0;
        } catch (TypeCheckerException exception) {
            System.err.println(exception.getMessage());
            result = 1;
        }
        final SemanticData semantics = typeChecker.getGeneratedData();
        // Print the contexts to a new file if context printing is enabled
        final boolean printAllContexts = line.hasOption("dumpsymtaball");
        if (printAllContexts || line.hasOption("dumpsymtab")) {
            final int printResult = printContexts(semantics, printAllContexts);
            if (printResult != 0) {
                return printResult;
            }
        }
        // If the type-checking failed, then abort pretty printing the types
        if (result != 0) {
            return result;
        }
        // Pretty print with types if enabled by the flags
        if (line.hasOption("pptype")) {
            return printAST(semantics);
        }
        // Otherwise return success
        return 0;
    }

    private int printAST(SemanticData semantics) {
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
            Golite.prettyPrint(ast, semantics, pretty);
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

    private int printContexts(SemanticData semantics, boolean all) {
        // Always print to a different file than the usual output
        final File contextOutputFile = setFileExtension(outputFile, SYMBOL_TABLE_EXTENSION);
        // Create the output writer
        final Writer pretty;
        try {
            pretty = new FileWriter(contextOutputFile);
        } catch (IOException exception) {
            System.err.println("Cannot create output file: " + exception.getMessage());
            return 1;
        }
        // Then do the context printing
        try {
            semantics.printContexts(new SourcePrinter(pretty), all);
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

    private CommandLine parseOptions(Option... options) {
        try {
            final Options optionsObject = new Options();
            for (Option option : options) {
                optionsObject.addOption(option);
            }
            final CommandLine line = new DefaultParser().parse(optionsObject, args);
            // Set the arguments to the remaining ones
            args = line.getArgs();
            return line;
        } catch (ParseException exception) {
            System.err.println("Invalid arguments: " + exception.getMessage());
            return null;
        }
    }

    private Option outputFileOption() {
        return Option.builder("o").hasArg().argName("file").desc("The output file").type(File.class).build();
    }

    private int getOutputFile(CommandLine line, String defaultExtension) {
        // Try to get it from the command line
        try {
            outputFile = (File) line.getParsedOptionValue("o");
            if (outputFile != null) {
                return 0;
            }
        } catch (ParseException exception) {
            System.err.println("Invalid arguments: " + exception.getMessage());
            return 1;
        }
        // Otherwise derive it from the input file
        outputFile = setFileExtension(inputFile, defaultExtension);
        return 0;
    }

    private static File setFileExtension(File file, String extension) {
        final String inputName = file.getName();
        final int index = inputName.indexOf('.');
        final String outputName;
        if (index < 0) {
            outputName = inputName + '.' + extension;
        } else {
            outputName = inputName.substring(0, index + 1) + extension;
        }
        return new File(file.getParent(), outputName);
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
