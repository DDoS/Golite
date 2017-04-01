package golite.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 *
 */
public class DefaultCommand extends Command {
    private static final String VERSION_OPTION = "v";

    public DefaultCommand() {
        super("");
    }

    @Override
    public String getHelp() {
        return "Commands: \"\" | \"parse\" | \"print\" | \"typecheck\" | \"irgen\" | \"codegen\" | \"compile\"\n" +
                "Use \"-h\" with any of these commands for further information";
    }

    @Override
    public void addCommandLineOptions(Options options) {
        options.addOption(new Option(VERSION_OPTION, "Print the version number"));
    }

    @Override
    public void execute(CommandLine commandLine) {
        System.out.println("Team 9 Golite compiler");
        System.out.println("v1.0.0-rc");
        if (!commandLine.hasOption(VERSION_OPTION)) {
            System.out.println(getHelp());
        }
    }

    @Override
    public void output(CommandLine commandLine) {
    }
}
