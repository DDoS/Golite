package golite.test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Iterator;

import golite.Golite;
import golite.semantic.SemanticException;
import golite.syntax.SyntaxException;
import org.junit.Assert;
import org.junit.Test;

public class SemanticsTest {
    @Test
    public void testBenchmarkPrograms() throws Exception {
        testPrograms(true, "benchmarks");
    }

    @Test
    public void testCodePrograms() throws Exception {
        testPrograms(true, "code");
    }

    @Test
    public void testExtraCodePrograms() throws Exception {
        testPrograms(true, "code_extra");
    }

    @Test
    public void testValidPrograms() throws Exception {
        testPrograms(true, "valid");
    }

    @Test
    public void testExtraValidPrograms() throws Exception {
        testPrograms(true, "valid_extra", "types");
    }

    @Test
    public void testInvalidPrograms() throws Exception {
        testPrograms(false, "invalid", "types");
    }

    @Test
    public void testExtraInvalidPrograms() throws Exception {
        testPrograms(false, "invalid_extra", "types");
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
                testSemantics(sourceFile);
            } else {
                testSemanticError(sourceFile);
            }
        }
    }

    private static void testSemantics(Path sourceFile) throws Exception {
        System.out.println("Testing: " + sourceFile.toString());
        Golite.typeCheck(Files.newBufferedReader(sourceFile));
    }

    private static void testSemanticError(Path sourceFile) throws IOException {
        System.out.println("Testing: " + sourceFile.toString());
        try {
            Golite.typeCheck(Files.newBufferedReader(sourceFile));
        } catch (SemanticException exception) {
            System.out.println("    " + exception.getMessage());
            return;
        } catch (SyntaxException e) {
            Assert.fail("Expected a semantic error, but got a syntax error");
            return;
        }
        Assert.fail("Expected a semantic error");
    }
}
