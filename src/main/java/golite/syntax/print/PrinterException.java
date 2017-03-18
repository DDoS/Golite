package golite.syntax.print;

/**
 * A runtime exception for the pretty printer, since we need to wrap the explicit IO exception if we want to use them in a Switch.
 */
public class PrinterException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public PrinterException(Throwable cause) {
        super(cause);
    }
}
