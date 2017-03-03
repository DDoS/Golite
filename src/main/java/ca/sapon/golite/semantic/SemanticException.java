package ca.sapon.golite.semantic;

import ca.sapon.golite.semantic.check.TypeCheckerException;

/**
 * An exception thrown during semantic validation.
 */
public class SemanticException extends Exception {
    private static final long serialVersionUID = 1;

    public SemanticException(TypeCheckerException cause) {
        super("Type checker error: " + cause.getMessage());
    }
}
