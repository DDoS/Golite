package golite.cli;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 */
public class Cli {
    private final Map<String, Command> commands = new HashMap<>();

    public Cli() {
        registerCommand(new DefaultCommand());
        registerCommand(new ParseCommand());
        registerCommand(new PrintCommand());
        registerCommand(new TypeCheckCommand());
        registerCommand(new IrGenerateCommand());
        registerCommand(new CodeGenerateCommand());
    }

    private void registerCommand(Command command) {
        commands.put(command.getName(), command);
    }

    public void execute(String[] args) {
        // Find the command to execute (default if none is found)
        final Command command;
        if (args.length < 1) {
            command = commands.get("");
        } else {
            final Command nonEmpty = commands.get(args[0]);
            if (nonEmpty != null) {
                command = nonEmpty;
                args = subArray(args, 1);
            } else {
                command = commands.get("");
            }
        }
        // Merge all the command options into one
        final Options options = new Options();
        Command parent = command;
        do {
            parent.addCommandLineOptions(options);
            parent = parent.getParent();
        } while (parent != null);
        // Get the output information for the command and its parents
        final List<OutputSetter> outputSetters = getOutputInfos(command, false);
        if (!outputSetters.isEmpty()) {
            options.addOption(Option.builder("o").hasArg().argName("file").desc("The output file").type(File.class).build());
        }
        // Parse the command line
        final CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse(options, args);
        } catch (ParseException exception) {
            throw new CommandException("Invalid arguments: " + exception.getMessage());
        }
        // Parse and set the input file argument (if any), and throw an error if any unused arguments are left
        final ArrayList<String> remainingArgs = new ArrayList<>(commandLine.getArgList());
        final File inputFile = setInputs(command, remainingArgs, null);
        if (!remainingArgs.isEmpty()) {
            throw new CommandException("Unexpected argument(s): " + remainingArgs);
        }
        // Now execute the commands, from the parent down
        execute(command, commandLine);
        // Now set the outputs
        final File outputFile;
        try {
            outputFile = (File) commandLine.getParsedOptionValue("o");
        } catch (ParseException exception) {
            throw new CommandException("Invalid argument: " + exception.getMessage());
        }
        outputSetters.forEach(setter -> setter.setOutput(inputFile, outputFile));
        // Almost done: produce the outputs, from the parent down
        output(command, commandLine);
        // Finally we close the output files
        outputSetters.forEach(OutputSetter::closeOutput);
    }

    private static List<OutputSetter> getOutputInfos(Command command, boolean parent) {
        final List<OutputSetter> outputSetters = new ArrayList<>();
        if (command.isOutputEnabled()) {
            for (Method method : command.getClass().getDeclaredMethods()) {
                final CommandOutput annotation = method.getAnnotation(CommandOutput.class);
                if (annotation == null) {
                    continue;
                }
                // Parent functions are always auxiliary output
                final boolean auxiliaryOutput = annotation.auxiliary() || parent;
                // Check that the output setter has the proper parameters, and create a function from the file to the parameter
                final Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length != 1) {
                    throw new IllegalStateException("Expected only one parameter for output setter");
                }
                final Class<?> paramType = parameters[0];
                final Function<File, Closeable> outputMapper;
                if (paramType == Writer.class) {
                    outputMapper = file -> {
                        try {
                            return new FileWriter(file);
                        } catch (IOException exception) {
                            throw new CommandException("Cannot create output file: " + exception.getMessage());
                        }
                    };
                } else if (paramType == FileOutputStream.class) {
                    outputMapper = file -> {
                        try {
                            return new FileOutputStream(file, false);
                        } catch (FileNotFoundException exception) {
                            throw new CommandException("Cannot create output file: " + exception.getMessage());
                        }
                    };
                } else {
                    throw new IllegalStateException("Only " + Writer.class.getCanonicalName() + " and "
                            + FileOutputStream.class.getCanonicalName() + " are supported as output");
                }
                // Create a closure for setting the output
                final Consumer<Closeable> outputSetter = output -> {
                    method.setAccessible(true);
                    try {
                        method.invoke(command, output);
                    } catch (IllegalAccessException | InvocationTargetException exception) {
                        throw new RuntimeException(exception);
                    }
                };
                outputSetters.add(new OutputSetter(annotation.extension(), auxiliaryOutput, outputMapper, outputSetter));
            }
        }
        if (command.getParent() != null) {
            outputSetters.addAll(getOutputInfos(command.getParent(), true));
        }
        return outputSetters;
    }

    private static File setInputs(Command command, List<String> args, File inputFile) {
        for (Method method : command.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(CommandInput.class)) {
                continue;
            }
            // Find the input file if missing
            if (inputFile == null) {
                if (args.size() < 1) {
                    throw new CommandException("Expected one input file, not " + args.size());
                }
                inputFile = new File(args.get(0));
                args.remove(0);
            }
            // Check that the input setter has the proper parameters
            final Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1) {
                throw new IllegalStateException("Expected only one parameter for input setter");
            }
            if (parameters[0] != Reader.class) {
                throw new IllegalStateException("Only " + Reader.class.getCanonicalName() + " is supported as input");
            }
            // Set the input
            try {
                method.invoke(command, new BufferedReader(new FileReader(inputFile)));
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new RuntimeException(exception);
            } catch (FileNotFoundException exception) {
                throw new CommandException("Input file not found: " + exception.getMessage());
            }
        }
        return command.getParent() != null ? setInputs(command.getParent(), args, inputFile) : inputFile;
    }

    private static void execute(Command command, CommandLine commandLine) {
        if (command.getParent() != null) {
            execute(command.getParent(), commandLine);
        }
        command.execute(commandLine);
    }

    private static void output(Command command, CommandLine commandLine) {
        if (command.getParent() != null) {
            output(command.getParent(), commandLine);
        }
        if (command.isOutputEnabled()) {
            command.output(commandLine);
        }
    }

    private static <T> T[] subArray(T[] array, int start) {
        int length = Array.getLength(array);
        final int newLength = length - start;
        @SuppressWarnings("unchecked")
        final T[] sub = (T[]) Array.newInstance(array.getClass().getComponentType(), newLength);
        System.arraycopy(array, start, sub, 0, newLength);
        return sub;
    }

    private static class OutputSetter {
        private final String extension;
        private final boolean auxiliary;
        private final Function<File, Closeable> outputMapper;
        private final Consumer<Closeable> outputSetter;
        private Closeable output;

        private OutputSetter(String extension, boolean auxiliary, Function<File, Closeable> outputMapper,
                             Consumer<Closeable> outputSetter) {
            this.extension = extension;
            this.auxiliary = auxiliary;
            this.outputMapper = outputMapper;
            this.outputSetter = outputSetter;
        }

        private void setOutput(File inputFile, File outputFile) {
            if (outputFile == null) {
                if (inputFile == null) {
                    throw new CommandException("Missing input file");
                }
                outputFile = setFileExtension(inputFile, extension);
            } else if (auxiliary) {
                outputFile = setFileExtension(outputFile, extension);
            }
            output = outputMapper.apply(outputFile);
            outputSetter.accept(output);
        }

        private void closeOutput() {
            try {
                output.close();
            } catch (IOException exception) {
                throw new CommandException("Error when closing output file: " + exception.getMessage());
            }
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
    }
}
