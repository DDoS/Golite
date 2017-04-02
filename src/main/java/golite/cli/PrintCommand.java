package golite.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import golite.Golite;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 *
 */
public class PrintCommand extends Command {
    private final ParseCommand parse = new ParseCommand();
    private File output;

    public PrintCommand() {
        super("print");
        setParent(parse);
        parse.setOutputEnabled(false);
    }

    @Override
    public String getHelp() {
        return "Pretty-prints the Golite program";
    }

    @Override
    public void addCommandLineOptions(Options options) {
    }

    @CommandOutput(extension = "pretty.go")
    public void setOutput(File output) {
        this.output = output;
    }

    @Override
    public void execute(CommandLine commandLine) {
    }

    @Override
    public void output(CommandLine commandLine) {
        try (Writer writer = new BufferedWriter(new FileWriter(output))) {
            Golite.prettyPrint(parse.getAst(), writer);
        } catch (Exception exception) {
            throw new CommandException("Error when printing", exception);
        }
    }
}
