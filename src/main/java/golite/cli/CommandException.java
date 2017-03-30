package golite.cli;

/**
 *
 */
public class CommandException extends RuntimeException {
    private static final long serialVersionUID = 0;

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
