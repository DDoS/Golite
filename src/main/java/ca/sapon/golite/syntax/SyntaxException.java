package ca.sapon.golite.syntax;

import ca.sapon.golite.syntax.weed.WeederException;
import golite.lexer.LexerException;
import golite.parser.ParserException;

/**
 * Combine lexer, parser and weeder exceptions.
 */
public class SyntaxException extends Exception {
    private static final long serialVersionUID = 1;

    public SyntaxException(LexerException cause) {
        super("Lexer error: " + cause.getMessage());
    }

    public SyntaxException(ParserException cause) {
        super("Parser error: " + cause.getMessage());
    }

    public SyntaxException(WeederException cause) {
        super("Weeder error: " + cause.getMessage());
    }
}
