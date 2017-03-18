package golite.util;

import java.io.IOException;
import java.io.Writer;

import golite.syntax.print.PrinterException;

/**
 * Prints source code to an writer stream.
 */
public class SourcePrinter {
    private static final String INDENTATION = "    ";
    private final Writer writer;
    private int indentationCount = 0;
    private boolean indentNext = false;

    public SourcePrinter(Writer writer) {
        this.writer = writer;
    }

    public SourcePrinter print(String str) {
        try {
            if (indentNext) {
                for (int i = 0; i < indentationCount; i++) {
                    writer.append(INDENTATION);
                }
                indentNext = false;
            }
            writer.append(str);
        } catch (IOException exception) {
            throw new PrinterException(exception);
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
            writer.append(System.lineSeparator());
        } catch (IOException exception) {
            throw new PrinterException(exception);
        }
        indentNext = true;
        return this;
    }
}
