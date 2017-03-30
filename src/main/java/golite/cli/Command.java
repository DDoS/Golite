package golite.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 *
 */
public abstract class Command {
    private final String name;
    private Command parent;
    private boolean outputEnabled = true;

    protected Command(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Command getParent() {
        return parent;
    }

    public void setParent(Command parent) {
        this.parent = parent;
    }

    public boolean isOutputEnabled() {
        return outputEnabled;
    }

    public void setOutputEnabled(boolean outputEnabled) {
        this.outputEnabled = outputEnabled;
    }

    public abstract void addCommandLineOptions(Options options);

    public abstract void execute(CommandLine commandLine);

    public abstract void output(CommandLine commandLine);
}
