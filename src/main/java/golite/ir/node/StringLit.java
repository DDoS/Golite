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
package golite.ir.node;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class StringLit extends Expr<BasicType> {
    private String value;
    private ByteBuffer utf8Data;

    public StringLit(String value) {
        super(BasicType.STRING);
        this.value = value;
        final byte[] data;
        try {
            data = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw new RuntimeException(exception);
        }
        utf8Data = ByteBuffer.allocateDirect(data.length).put(data);
        utf8Data.flip();
    }

    public String getValue() {
        return value;
    }

    public ByteBuffer getUtf8Data() {
        return utf8Data;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitStringLit(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        final StringBuilder escaped = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (Character.isISOControl(c) || (Character.isSpaceChar(c) && c != ' ')) {
                escaped.append("\\").append(String.format("%02X", (int) c));
            } else if (c == '\"') {
                escaped.append("\\\"");
            } else {
                escaped.append(c);
            }
        }
        printer.print("\"").print(escaped.toString()).print("\"");
    }
}
