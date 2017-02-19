package ca.sapon.golite.test;

import java.io.BufferedReader;
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
            final BufferedReader in = Files.newBufferedReader(sourceFile);
            final String firstPass = Golite.prettyPrint(in);
            final String secondPass = Golite.prettyPrint(new StringReader(firstPass));
            Assert.assertEquals(firstPass, secondPass);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
