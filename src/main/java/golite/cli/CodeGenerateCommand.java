package golite.cli;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import golite.codegen.CodeGenerator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 *
 */
public class CodeGenerateCommand extends Command {
    private final IrGenerateCommand irGenerate = new IrGenerateCommand();
    private ByteBuffer bitCode;
    private FileOutputStream output;

    public CodeGenerateCommand() {
        super("codegen");
        setParent(irGenerate);
        irGenerate.setOutputEnabled(false);
    }

    @Override
    public void addCommandLineOptions(Options options) {
    }

    @CommandOutput(extension = "o")
    public void setOutput(FileOutputStream output) {
        this.output = output;
    }

    @Override
    public void execute(CommandLine commandLine) {
        final CodeGenerator codeGenerator = new CodeGenerator();
        try {
            irGenerate.getProgram().visit(codeGenerator);
        } catch (Exception exception) {
            throw new CommandException("Error when generating code", exception);
        }
        bitCode = codeGenerator.getMachineCode();
    }

    @Override
    public void output(CommandLine commandLine) {
        try {
            output.getChannel().write(bitCode);
        } catch (FileNotFoundException exception) {
            throw new CommandException("Error when creating bit code output file: " + exception.getMessage());
        } catch (IOException exception) {
            throw new CommandException("Error when writing bit code: " + exception.getMessage());
        }
    }
}
