package ca.sapon.golite;

import java.io.ByteArrayOutputStream;
import java.io.PushbackReader;
import java.io.Reader;

import golite.lexer.Lexer;
import golite.node.Start;
import golite.parser.Parser;

/**
 * Methods for processing Golite code.
 */
public final class Golite {
    private Golite() throws Exception {
        throw new Exception("No");
    }

    public static Start parse(Reader source) {
        final Lexer lexer = new GoliteLexer(new PushbackReader(source, 4096));
        final Parser parser = new Parser(lexer);
        try {
            return parser.parse();
        } catch (Exception exception) {
           throw new RuntimeException(exception);
        }
    }

    public static String prettyPrint(Reader source) {
        return prettyPrint(parse(source));
    }

    public static String prettyPrint(Start ast) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final PrettyPrinter printer = new PrettyPrinter(output);
        ast.apply(printer);
        return output.toString();
    }
}
