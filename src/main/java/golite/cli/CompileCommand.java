package golite.cli;

import java.io.File;
import java.io.IOException;
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
    public static final File DEFAULT_RUNTIME_PATH = new File("build/objs/golite_runtime.o");
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

    public static void compileAndLink(ByteBuffer nativeCode, File runtimePath, File ouputFile) {
        // Create a temporary file for the program object
        final File programObjectFile;
        try {
            programObjectFile = File.createTempFile("golite", ".o");
        } catch (IOException exception) {
            throw new CommandException("Error when creating the temporary object file: " + exception.getMessage());
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
        } catch (IOException exception) {
            throw new CommandException("Error when writing native code: " + exception.getMessage());
        }
    }

    private static void execute(String... command) {
        final ProcessBuilder processBuilder = new ProcessBuilder(command)
                .redirectOutput(Redirect.INHERIT).redirectError(Redirect.INHERIT);
        final Process process;
        try {
            process = processBuilder.start();
        } catch (IOException exception) {
            throw new CommandException("Error when trying to execute " + command[0] + ": " + exception.getMessage());
        }
        try {
            if (process.waitFor() != 0) {
                throw new CommandException(command[0] + " terminated with a non-zero exit code");
            }
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
