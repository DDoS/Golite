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

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 */
public class CompileCommand extends Command {
    private static final String NO_LINK_OPTION = "c";
    private static final String RUNTIME_PATH = "l";
    private static final String RUN_OPTION = "r";
    private static final File DEFAULT_RUNTIME_PATH = new File("build/objs/golite_runtime.o");
    private final CodeGenerateCommand codeGenerate = new CodeGenerateCommand();
    private File runtimePath;
    private ByteBuffer nativeCode;
    private File objectOutput;
    private File executableOutput;

    public CompileCommand() {
        super("compile");
        setParent(codeGenerate);
        codeGenerate.setOutputEnabled(false);
    }

    @CommandOutput(extension = "o")
    public void setOutputObject(File output) {
        objectOutput = output;
    }

    @CommandOutput(extension = "out")
    public void setOutputExecutable(File output) {
        executableOutput = output;
    }

    @Override
    public String getHelp() {
        return "Compile the program into an object or executable binary";
    }

    @Override
    public void addCommandLineOptions(Options options) {
        final OptionGroup group = new OptionGroup();
        group.addOption(new Option(NO_LINK_OPTION, "Don't link; just output the object file"));
        group.addOption(new Option(RUN_OPTION, "Run the executable after linking"));
        options.addOptionGroup(group);
        options.addOption(Option.builder(RUNTIME_PATH).hasArg().argName("runtime")
                .desc("The runtime object").type(File.class).build());
    }

    @Override
    public void execute(CommandLine commandLine) {
        // Get the path to runtime (default if not present)
        runtimePath = DEFAULT_RUNTIME_PATH;
        try {
            final Object optionValue = commandLine.getParsedOptionValue(RUNTIME_PATH);
            if (optionValue != null) {
                runtimePath = (File) optionValue;
            }
        } catch (ParseException exception) {
            throw new CommandException("Invalid argument: " + exception.getMessage());
        }
        // Check that the runtime path is valid
        if (!runtimePath.exists()) {
            throw new CommandException("The runtime object does not exist: " + runtimePath);
        }
        // Get the native code
        try {
            nativeCode = codeGenerate.getCode().asNativeCode();
        } catch (Exception exception) {
            throw new CommandException("Error when generating native code", exception);
        }
    }

    @Override
    public void output(CommandLine commandLine) {
        if (commandLine.hasOption(NO_LINK_OPTION)) {
            writeNativeCode(nativeCode, objectOutput);
        } else {
            compileAndLink(nativeCode, runtimePath, executableOutput);
            if (commandLine.hasOption(RUN_OPTION)) {
                execute(executableOutput.getAbsolutePath());
            }
        }
    }

    private static void compileAndLink(ByteBuffer nativeCode, File runtimePath, File ouputFile) {
        // Create a temporary file for the program object
        final File programObjectFile;
        try {
            programObjectFile = File.createTempFile("golite", ".o");
        } catch (Exception exception) {
            throw new CommandException("Error when creating the temporary object file", exception);
        }
        programObjectFile.deleteOnExit();
        // Write the native code to it
        writeNativeCode(nativeCode, programObjectFile);
        // Now use CC to link it with the runtime
        execute("cc", runtimePath.getAbsolutePath(), programObjectFile.getAbsolutePath(),
                "-o", ouputFile.getAbsolutePath());
    }

    private static void writeNativeCode(ByteBuffer nativeCode, File outputFile) {
        try (FileChannel channel = FileChannel.open(outputFile.toPath(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            channel.write(nativeCode);
        } catch (Exception exception) {
            throw new CommandException("Error when writing native code", exception);
        }
    }

    private static void execute(String... command) {
        final ProcessBuilder processBuilder = new ProcessBuilder(command)
                .redirectOutput(Redirect.INHERIT).redirectError(Redirect.INHERIT);
        final Process process;
        try {
            process = processBuilder.start();
        } catch (Exception exception) {
            throw new CommandException("Error when trying to execute " + command[0], exception);
        }
        try {
            if (process.waitFor() != 0) {
                throw new CommandException(command[0] + " terminated with a non-zero exit code");
            }
        } catch (InterruptedException exception) {
            throw new CommandException("Interrupted while waiting for " + command[0] + " to terminate", exception);
        }
    }
}
