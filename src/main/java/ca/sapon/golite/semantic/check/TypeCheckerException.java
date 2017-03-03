package ca.sapon.golite.semantic.check;

import ca.sapon.golite.util.SourcePositioned;

/**
 * An exception throw by the type checker.
 */
public class TypeCheckerException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public TypeCheckerException(SourcePositioned source, String message) {
        super(SourcePositioned.appendPosition(source, message));
    }
}
