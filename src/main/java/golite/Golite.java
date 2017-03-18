package golite;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;

import golite.semantic.SemanticData;
import golite.semantic.SemanticException;
import golite.semantic.check.TypeChecker;
import golite.semantic.check.TypeCheckerException;
import golite.syntax.GoliteLexer;
import golite.syntax.SyntaxException;
import golite.syntax.print.PrettyPrinter;
import golite.syntax.print.PrinterException;
import golite.syntax.weed.Weeder;
import golite.syntax.weed.WeederException;
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
        prettyPrint(ast, null, pretty);
    }

    public static void prettyPrint(Start ast, SemanticData semantics, Writer pretty) throws PrinterException {
        ast.apply(new PrettyPrinter(semantics, pretty));
    }

    public static SemanticData typeCheck(Reader source) throws IOException, SyntaxException, SemanticException {
        return typeCheck(parse(source));
    }

    public static SemanticData typeCheck(Start ast) throws SemanticException {
        try {
            final TypeChecker typeChecker = new TypeChecker();
            ast.apply(typeChecker);
            return typeChecker.getGeneratedData();
        } catch (TypeCheckerException exception) {
            throw new SemanticException(exception);
        }
    }
}
