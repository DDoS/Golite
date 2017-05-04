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
package golite.semantic.symbol;

import golite.semantic.type.Type;
import golite.util.SourcePositioned;

/**
 * A symbol generated by any declaration.
 */
public abstract class Symbol<T extends Type> implements SourcePositioned {
    private final int startLine, endLine;
    private final int startPos, endPos;
    protected final String name;

    protected Symbol(String name) {
        this(0, 0, 0, 0, name);
    }

    protected Symbol(int startLine, int endLine, int startPos, int endPos, String name) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.startPos = startPos;
        this.endPos = endPos;
        this.name = name;
    }

    @Override
    public int getStartLine() {
        return startLine;
    }

    @Override
    public int getEndLine() {
        return endLine;
    }

    @Override
    public int getStartPos() {
        return startPos;
    }

    @Override
    public int getEndPos() {
        return endPos;
    }

    public String getName() {
        return name;
    }

    public abstract T getType();
}
