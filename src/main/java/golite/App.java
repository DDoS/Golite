package golite;

import golite.cli.Cli;
import golite.cli.CommandException;

public final class App {
    private App() throws Exception {
        throw new Exception("NO!");
    }

    public static void main(String[] args) {
        final Cli cli = new Cli();
        try {
            cli.execute(args);
            System.exit(0);
        } catch (CommandException exception) {
            System.err.println(exception.getMessage());
            if (exception.getCause() != null) {
                exception.getCause().printStackTrace(System.err);
            }
            System.exit(1);
        }
    }
}
