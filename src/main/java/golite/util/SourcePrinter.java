/*
 * This file is part of GoLite, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017 Aleksi Sapon, Rohit Verma, Ayesha Krishnamurthy <https://github.com/DDoS/Golite>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package golite.util;

import java.io.IOException;
import java.io.Writer;

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
            writer.append(System.lineSeparator());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        indentNext = true;
        return this;
    }
}
