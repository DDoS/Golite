package golite.test;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Iterator;

import golite.Golite;
import golite.syntax.SyntaxException;
import org.junit.Assert;
import org.junit.Test;

public class SyntaxTest {
    @Test
    public void testValidPrograms() throws Exception {
        testPrograms(true, "valid");
    }

    @Test
    public void testExtraValidPrograms() throws Exception {
        testPrograms(true, "valid_extra", "syntax");
    }

    @Test
    public void testInvalidPrograms() throws Exception {
        testPrograms(false, "invalid", "syntax");
    }

    @Test
    public void testExtraInvalidPrograms() throws Exception {
        testPrograms(false, "invalid_extra", "syntax");
    }

    private static void testPrograms(boolean valid, String... path) throws Exception {
        final Path validDirectory = FileSystems.getDefault().getPath("programs", path);
        final PathMatcher goliteMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.go");
        final Iterator<Path> files = Files.list(validDirectory).filter(goliteMatcher::matches).iterator();
        if (!files.hasNext()) {
            throw new Exception("Expected at least one test file");
        }
        while (files.hasNext()) {
            final Path sourceFile = files.next();
            if (valid) {
                testPrinterInvariant(sourceFile);
            } else {
                testSyntaxError(sourceFile);
            }
        }
    }

    private static void testPrinterInvariant(Path sourceFile) throws Exception {
        System.out.println("Testing: " + sourceFile.toString());
        final Reader source = Files.newBufferedReader(sourceFile);
        final Writer firstOut = new CharArrayWriter();
        Golite.prettyPrint(source, firstOut);
        final String firstPass = firstOut.toString();
        final Writer secondOut = new CharArrayWriter();
        Golite.prettyPrint(new StringReader(firstPass), secondOut);
        Assert.assertEquals(firstPass, secondOut.toString());
    }

    private static void testSyntaxError(Path sourceFile) throws IOException {
        System.out.println("Testing: " + sourceFile.toString());
        final Reader source = Files.newBufferedReader(sourceFile);
        try {
            Golite.parse(source);
        } catch (SyntaxException exception) {
            System.out.println("    " + exception.getMessage());
            return;
        }
        Assert.fail("Expected a syntax error");
    }
}
