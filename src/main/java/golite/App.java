package golite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import golite.codegen.CodeGenerator;
import golite.ir.IrConverter;
import golite.ir.node.Program;
import golite.node.Start;
import golite.semantic.SemanticData;
import golite.semantic.check.TypeChecker;
import golite.semantic.check.TypeCheckerException;
import golite.syntax.SyntaxException;
import golite.syntax.print.PrinterException;
import golite.util.SourcePrinter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public final class App {
    private static final String PRETTY_EXTENSION = "pretty.go";
    private static final String SYMBOL_TABLE_EXTENSION = "symtab";
    private static final String TYPE_ANNOTATION_EXTENSION = "pptype.go";
    private static final String IR_EXTENSION = "ir";
    private static final String CODE_EXTENSION = "bc";
    private String[] args;
    private File inputFile = null;
    private File outputFile = null;
    private Start ast = null;
    private SemanticData semantics = null;
    private Program program = null;

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
            case "irgen":
                return irgenCommand();
            case "codegen":
                return codegenCommand();
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
        result = findOutputFile(line);
        if (result != 0) {
            return result;
        }
        // Finally do the printing
        return printAST(deriveOutputFile(PRETTY_EXTENSION));
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
        result = findOutputFile(line);
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
        semantics = typeChecker.getGeneratedData();
        // Print the contexts to a new file if context printing is enabled
        final boolean printAllContexts = line.hasOption("dumpsymtaball");
        if (printAllContexts || line.hasOption("dumpsymtab")) {
            final int printResult = printContexts(setFileExtension(outputFile, SYMBOL_TABLE_EXTENSION), printAllContexts);
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
            return printAST(setFileExtension(outputFile, TYPE_ANNOTATION_EXTENSION));
        }
        // Otherwise return success
        return 0;
    }

    private int irgenCommand() {
        // Create the IR
        final int result = generateIr();
        if (result != 0) {
            return result;
        }
        // Create the output writer
        final Writer pretty;
        try {
            pretty = new FileWriter(deriveOutputFile(IR_EXTENSION));
        } catch (IOException exception) {
            System.err.println("Cannot create output file: " + exception.getMessage());
            return 1;
        }
        // Then print the IR
        try {
            program.print(new SourcePrinter(pretty));
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

    private int codegenCommand() {
        // Create the IR
        final int result = generateIr();
        if (result != 0) {
            return result;
        }
        // Then do the code generation
        final CodeGenerator codeGenerator = new CodeGenerator();
        try {
            program.visit(codeGenerator);
        } catch (Exception exception) {
            //System.err.println("Error when generating code: " + exception.getMessage());
            exception.printStackTrace();
            return 1;
        }
        final ByteBuffer bitCode = codeGenerator.getBitCode();
        // Write the code to a file
        final File codeOutputFile = deriveOutputFile(CODE_EXTENSION);
        try {
            final FileChannel channel = new FileOutputStream(codeOutputFile, false).getChannel();
            channel.write(bitCode);
            channel.close();
        } catch (FileNotFoundException exception) {
            System.err.println("Error when creating bit code output file: " + exception.getMessage());
            return 1;
        } catch (IOException exception) {
            System.err.println("Error when writing bit code: " + exception.getMessage());
            return 1;
        }
        return 0;
    }

    private int generateIr() {
        // Start with the type-checking
        final int result = typecheckCommand();
        if (result != 0) {
            return result;
        }
        // Then generate the IR
        final IrConverter irConverter = new IrConverter(semantics);
        try {
            ast.apply(irConverter);
        } catch (Exception exception) {
            //System.err.println("Error when generating IR: " + exception.getMessage());
            exception.printStackTrace();
            return 1;
        }
        program = irConverter.getProgram();
        return 0;
    }

    private int printAST(File astOutputFile) {
        // Create the output writer
        final Writer pretty;
        try {
            pretty = new FileWriter(astOutputFile);
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

    private int printContexts(File contextOutputFile, boolean all) {
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

    private int findOutputFile(CommandLine line) {
        try {
            outputFile = (File) line.getParsedOptionValue("o");
            return 0;
        } catch (ParseException exception) {
            System.err.println("Invalid arguments: " + exception.getMessage());
            return 1;
        }
    }

    private File deriveOutputFile(String defaultExtension) {
        return outputFile != null ? outputFile : setFileExtension(inputFile, defaultExtension);
    }

    private static File setFileExtension(File file, String extension) {
        final String inputName = file.getName();
        final int index = inputName.lastIndexOf('.');
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
