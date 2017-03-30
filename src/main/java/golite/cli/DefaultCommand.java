package golite.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

/**
 *
 */
public class DefaultCommand extends Command {
    private static final String HELP_OPTION = "h";
    private static final String VERSION_OPTION = "v";

    public DefaultCommand() {
        super("");
    }

    @Override
    public void addCommandLineOptions(Options options) {
        final OptionGroup group = new OptionGroup()
                .addOption(new Option(HELP_OPTION, "print the help dialog"))
                .addOption(new Option(VERSION_OPTION, "print the version number"));
        group.setRequired(true);
        options.addOptionGroup(group);
    }

    @Override
    public void execute(CommandLine commandLine) {
        if (commandLine.hasOption(HELP_OPTION)) {
            // TODO: print help
            System.out.println("HELP");
        } else if (commandLine.hasOption(VERSION_OPTION)) {
            // TODO: print version
            System.out.println("VERSION");
        }
    }

    @Override
    public void output(CommandLine commandLine) {
    }
}
