package golite.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

/**
 *
 */
public class DefaultCommand extends Command {
    public DefaultCommand() {
        super("");
    }

    @Override
    public void addCommandLineOptions(Options options) {
        final OptionGroup group = new OptionGroup()
                .addOption(new Option("h", "print the help dialog"))
                .addOption(new Option("v", "print the version number"));
        group.setRequired(true);
        options.addOptionGroup(group);
    }

    @Override
    public void execute(CommandLine commandLine) {
        if (commandLine.hasOption('h')) {
            // TODO: print help
            System.out.println("HELP");
        } else if (commandLine.hasOption('v')) {
            // TODO: print version
            System.out.println("VERSION");
        }
    }

    @Override
    public void output(CommandLine commandLine) {
    }
}
