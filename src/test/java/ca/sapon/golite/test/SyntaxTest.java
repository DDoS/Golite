package ca.sapon.golite.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import ca.sapon.golite.PrettyPrinter;
import golite.lexer.Lexer;
import golite.node.Start;
import golite.parser.Parser;
import org.junit.Assert;
import org.junit.Test;

public class SyntaxTest {
    @Test
    public void testValidPrograms() throws Exception {
        final Path validDirectory = FileSystems.getDefault().getPath("program", "valid");
        final PathMatcher goliteMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.golite");
        Files.list(validDirectory).filter(goliteMatcher::matches).forEach(SyntaxTest::testPrinterInvariant);
    }

    private static void testPrinterInvariant(Path sourceFile) {
        try {
            final BufferedReader in = Files.newBufferedReader(sourceFile);
            final String firstPass = prettyPrint(in);
            final String secondPass = prettyPrint(new StringReader(firstPass));
            Assert.assertEquals(firstPass, secondPass);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static String prettyPrint(Reader programSource) throws Exception {
        final Lexer lexer = new Lexer(new PushbackReader(programSource));
        final Parser parser = new Parser(lexer);
        final Start ast = parser.parse();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final PrettyPrinter printer = new PrettyPrinter(output);
        ast.apply(printer);
        return output.toString();
    }
}
