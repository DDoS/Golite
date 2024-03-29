/*
 * This file is part of GoLite, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017 Aleksi Sapon, Rohit Verma, Ayesha Krishnamurthy <https://github.com/DDoS/Golite>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package golite.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 */
public class Cli {
    private static final String HELP_OPTION = "h";
    private final Map<String, Command> commands = new HashMap<>();

    public Cli() {
        registerCommand(new DefaultCommand());
        registerCommand(new ParseCommand());
        registerCommand(new PrintCommand());
        registerCommand(new TypeCheckCommand());
        registerCommand(new IrGenerateCommand());
        registerCommand(new CodeGenerateCommand());
        registerCommand(new CompileCommand());
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
        // Add the help option
        options.addOption(HELP_OPTION, "Print the help information");
        // Get the output information for the command and its parents
        final List<OutputSetter> outputSetters = getOutputInfos(command, false);
        if (!outputSetters.isEmpty()) {
            // Add the output option if any command has an output
            options.addOption(Option.builder("o").hasArg().argName("file")
                    .desc("The output file").type(File.class).build());
        }
        // Parse the command line
        final CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse(options, args);
        } catch (ParseException exception) {
            throw new CommandException("Invalid arguments: " + exception.getMessage());
        }
        // If the help option is specified, then reject and other options and print the help
        if (commandLine.hasOption(HELP_OPTION)) {
            printHelp(command, options, commandLine);
            return;
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
                // Check that the output setter has the proper parameter
                final Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length != 1) {
                    throw new IllegalStateException("Expected only one parameter for output setter");
                }
                if (parameters[0] != File.class) {
                    throw new IllegalStateException("Only " + File.class.getCanonicalName() + " is supported as output");
                }
                // Create a closure for setting the output
                final Consumer<File> outputSetter = output -> {
                    method.setAccessible(true);
                    try {
                        method.invoke(command, output);
                    } catch (IllegalAccessException | InvocationTargetException exception) {
                        throw new RuntimeException(exception);
                    }
                };
                outputSetters.add(new OutputSetter(annotation.extension(), auxiliaryOutput, outputSetter));
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

    private static void printHelp(Command command, Options options, CommandLine commandLine) {
        final Option[] allOptions = commandLine.getOptions();
        if (allOptions.length > 1) {
            final List<String> optionsStrings = Stream.of(allOptions)
                    .map(option -> '-' + option.getOpt()).collect(Collectors.toList());
            throw new CommandException("Unexpected option(s): " + optionsStrings);
        }
        if (commandLine.getArgList().size() > 0) {
            throw new CommandException("Unexpected argument(s): " + commandLine.getArgList());
        }
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(100, "./run.sh " + command.getName(), command.getHelp() + '\n',
                options, "", true);
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

    private static class OutputSetter {
        private final String extension;
        private final boolean auxiliary;
        private final Consumer<File> outputSetter;

        private OutputSetter(String extension, boolean auxiliary, Consumer<File> outputSetter) {
            this.extension = extension;
            this.auxiliary = auxiliary;
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
            outputSetter.accept(outputFile);
        }
    }
}
