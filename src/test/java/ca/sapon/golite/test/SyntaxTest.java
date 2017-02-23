package ca.sapon.golite.test;

import java.io.CharArrayWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Iterator;

import ca.sapon.golite.Golite;
import org.junit.Assert;
import org.junit.Test;

public class SyntaxTest {
    @Test
    public void testValidPrograms() throws Exception {
        final Path validDirectory = FileSystems.getDefault().getPath("program", "valid");
        final PathMatcher goliteMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.go");
        for (Iterator<Path> files = Files.list(validDirectory).filter(goliteMatcher::matches).iterator(); files.hasNext(); ) {
            testPrinterInvariant(files.next());
        }
    }

    private static void testPrinterInvariant(Path sourceFile) throws Exception {
        final Reader source = Files.newBufferedReader(sourceFile);
        final Writer firstOut = new CharArrayWriter();
        Golite.prettyPrint(source, firstOut);
        final String firstPass = firstOut.toString();
        final Writer secondOut = new CharArrayWriter();
        Golite.prettyPrint(new StringReader(firstPass), secondOut);
        Assert.assertEquals(firstPass, secondOut.toString());
    }
}
