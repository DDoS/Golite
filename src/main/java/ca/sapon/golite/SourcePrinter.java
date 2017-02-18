package ca.sapon.golite;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Prints source code to an output stream.
 */
public class SourcePrinter {
    private static final String INDENTATION = "    ";
    private final OutputStream output;
    private int indentationCount = 0;
    private boolean indentNext = false;

    public SourcePrinter(OutputStream output) {
        this.output = output;
    }

    public SourcePrinter print(String str) {
        try {
            if (indentNext) {
                for (int i = 0; i < indentationCount; i++) {
                    output.write(INDENTATION.getBytes());
                }
                indentNext = false;
            }
            output.write(str.getBytes());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return this;
    }

    public SourcePrinter indent() {
        indentationCount++;
        return this;
    }

    public SourcePrinter dedent() {
        if (indentationCount <= 0) {
            throw new IllegalStateException("Cannot dedent more");
        }
        indentationCount--;
        return this;
    }

    public SourcePrinter newLine() {
        try {
            output.write(System.lineSeparator().getBytes());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        indentNext = true;
        return this;
    }
}
