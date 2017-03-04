package ca.sapon.golite;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;

import ca.sapon.golite.semantic.SemanticException;
import ca.sapon.golite.semantic.check.TypeChecker;
import ca.sapon.golite.semantic.check.TypeCheckerException;
import ca.sapon.golite.syntax.GoliteLexer;
import ca.sapon.golite.syntax.SyntaxException;
import ca.sapon.golite.syntax.print.PrettyPrinter;
import ca.sapon.golite.syntax.print.PrinterException;
import ca.sapon.golite.syntax.weed.Weeder;
import ca.sapon.golite.syntax.weed.WeederException;
import golite.lexer.Lexer;
import golite.lexer.LexerException;
import golite.node.Start;
import golite.parser.Parser;
import golite.parser.ParserException;

/**
 * Methods for processing Golite code.
 */
public final class Golite {
    private Golite() throws Exception {
        throw new Exception("No");
    }

    public static Start parse(Reader source) throws IOException, SyntaxException {
        final Lexer lexer = new GoliteLexer(new PushbackReader(source, 4096));
        final Parser parser = new Parser(lexer);
        try {
            final Start ast = parser.parse();
            ast.apply(new Weeder());
            return ast;
        } catch (LexerException exception) {
            throw new SyntaxException(exception);
        } catch (ParserException exception) {
            throw new SyntaxException(exception);
        } catch (WeederException exception) {
            throw new SyntaxException(exception);
        }
    }

    public static void prettyPrint(Reader source, Writer pretty) throws IOException, SyntaxException, PrinterException {
        prettyPrint(parse(source), pretty);
    }

    public static void prettyPrint(Start ast, Writer pretty) throws PrinterException {
        ast.apply(new PrettyPrinter(pretty));
    }

    public static void typeCheck(Reader source) throws IOException, SyntaxException, SemanticException {
        typeCheck(parse(source));
    }

    public static void typeCheck(Start ast) throws SemanticException {
        try {
            ast.apply(new TypeChecker());
        } catch (TypeCheckerException exception) {
            throw new SemanticException(exception);
        }
    }
}
