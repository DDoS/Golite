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

    private App() throws Exception {
        throw new Exception("No");
    }

    public static void main(String[] args) {
        int exitCode;
        if (args.length < 1) {
            System.err.println("Expected a command");
            exitCode = 1;
        } else {
            exitCode = executeCommands(args);
        }
        System.exit(exitCode);
    }

    private static int executeCommands(String[] args) {
        // Find the command to execute
        final String command = args[0];
        final Executor executor;
        switch (command) {
            case "print":
                executor = App::printCommand;
                break;
            default:
                System.err.println("Not a command: " + command);
                return 1;
        }
        args = subArray(args, 1);
        // Find the input and output files
        final File inputFile;
        final File outputFile;
        final String[] commandArgs;
        try {
            final CommandLine line = new DefaultParser().parse(buildOptions(), args);
            commandArgs = line.getArgs();
            if (commandArgs.length != 1) {
                System.err.println("Expected one input file, not " + commandArgs.length);
                return 1;
            }
            inputFile = new File(commandArgs[0]);
            outputFile = (File) line.getParsedOptionValue("output");
        } catch (ParseException exception) {
            System.err.println("Invalid arguments: " + exception.getMessage());
            return 1;
        }
        // Execute the command
        return executor.execute(inputFile, outputFile);
    }

    private static Options buildOptions() {
        final Options options = new Options();
        final Option outputFileOption = Option.builder("o").longOpt("output").hasArg().argName("file")
                .desc("The output file").type(File.class).build();
        options.addOption(outputFileOption);
        return options;
    }

    private static int printCommand(File inputFile, File outputFile) {
        // Derive the output file from the input one if missing
        if (outputFile == null) {
            outputFile = defaultOutputFile(inputFile, PRETTY_EXTENSION);
        }
        // Create the source reader
        final Reader source;
        try {
            source = new BufferedReader(new FileReader(inputFile));
        } catch (FileNotFoundException exception) {
            System.err.println("Input file not found: " + exception.getMessage());
            return 1;
        }
        // First do the lexing, parsing and weeding
        final Start ast;
        try {
            ast = Golite.parse(source);
        } catch (SyntaxException exception) {
            System.err.println(exception.getMessage());
            return 1;
        } catch (IOException exception) {
            System.err.println("Error when reading source: " + exception.getMessage());
            return 1;
        }
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
        // Close the files
        try {
            source.close();
        } catch (IOException exception) {
            System.err.println("Error when closing input file: " + exception.getMessage());
            return 1;
        }
        try {
            pretty.close();
        } catch (IOException exception) {
            System.err.println("Error when closing output file: " + exception.getMessage());
            return 1;
        }
        return 0;
    }

    private static File defaultOutputFile(File inputFile, String extension) {
        final String inputName = inputFile.getName();
        final int index = inputName.lastIndexOf('.');
        final String outputName;
        if (index < 0) {
            outputName = inputName + '.' + extension;
        } else {
            outputName = inputName.substring(0, index + 1) + extension;
        }
        return new File(inputFile.getParent(), outputName);
    }

    private static <T> T[] subArray(T[] array, int start) {
        int length = Array.getLength(array);
        final int newLength = length - start;
        @SuppressWarnings("unchecked")
        final T[] sub = (T[]) Array.newInstance(array.getClass().getComponentType(), newLength);
        System.arraycopy(array, start, sub, 0, newLength);
        return sub;
    }

    @FunctionalInterface
    private interface Executor {
        int execute(File inputFile, File outputFile);
    }
}
