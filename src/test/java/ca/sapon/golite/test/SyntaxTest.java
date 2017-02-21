package ca.sapon.golite.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import ca.sapon.golite.Golite;
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
            final BufferedReader source = Files.newBufferedReader(sourceFile);
            final ByteArrayOutputStream firstOut = new ByteArrayOutputStream();
            Golite.prettyPrint(source, firstOut);
            final String firstPass = firstOut.toString();
            final ByteArrayOutputStream secondOut = new ByteArrayOutputStream();
            Golite.prettyPrint(new StringReader(firstPass), secondOut);
            Assert.assertEquals(firstPass, secondOut.toString());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
