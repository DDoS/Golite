package ca.sapon.golite;

/**
 * An exception thrown be the weeder.
 */
public class WeederException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public WeederException(String message) {
        super(message);
    }
}
