package golite.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import golite.ir.IrConverter;
import golite.ir.node.Program;
import golite.util.SourcePrinter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 *
 */
public class IrGenerateCommand extends Command {
    private final TypeCheckCommand typeCheck = new TypeCheckCommand();
    private Program program;
    private File output;

    public IrGenerateCommand() {
        super("irgen");
        setParent(typeCheck);
        typeCheck.setOutputEnabled(true);
    }

    public Program getProgram() {
        return program;
    }

    @Override
    public String getHelp() {
        return "Generates a custom IR used for code generation";
    }

    @Override
    public void addCommandLineOptions(Options options) {
    }

    @CommandOutput(extension = "ir")
    public void setOutput(File output) {
        this.output = output;
    }

    @Override
    public void execute(CommandLine commandLine) {
        final IrConverter irConverter = new IrConverter(typeCheck.getSemantics());
        try {
            typeCheck.getAst().apply(irConverter);
        } catch (Exception exception) {
            throw new CommandException("Error when generating IR", exception);
        }
        program = irConverter.getProgram();
    }

    @Override
    public void output(CommandLine commandLine) {
        try (Writer writer = new BufferedWriter(new FileWriter(output))) {
            program.print(new SourcePrinter(writer));
        } catch (Exception exception) {
            throw new CommandException("Error when printing IR", exception);
        }
    }
}
