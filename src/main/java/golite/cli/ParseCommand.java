package golite.cli;

import java.io.IOException;
import java.io.Reader;

import golite.Golite;
import golite.node.Start;
import golite.syntax.SyntaxException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 *
 */
public class ParseCommand extends Command {
    private Reader input;
    private Start ast;

    public ParseCommand() {
        super("parse");
    }

    public Start getAst() {
        return ast;
    }

    @Override
    public void addCommandLineOptions(Options options) {
    }

    @CommandInput
    public void setInput(Reader input) {
        this.input = input;
    }

    @Override
    public void execute(CommandLine commandLine) {
        try {
            ast = Golite.parse(input);
        } catch (SyntaxException exception) {
            throw new CommandException(exception.getMessage());
        } catch (IOException exception) {
            throw new CommandException("Error when reading source: " + exception.getMessage());
        }
    }

    @Override
    public void output(CommandLine commandLine) {
    }
}
