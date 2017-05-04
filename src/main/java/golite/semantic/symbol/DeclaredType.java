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

import golite.semantic.type.AliasType;
import golite.semantic.type.Type;
import golite.util.SourcePositioned;

/**
 * The symbol generated by a type declaration.
 */
public class DeclaredType<T extends Type> extends Symbol<T> {
    private final T type;

    public DeclaredType(String name, T type) {
        super(name);
        this.type = type;
    }

    public DeclaredType(SourcePositioned source, String name, T type) {
        this(source.getStartLine(), source.getEndLine(), source.getStartPos(), source.getEndPos(), name, type);
    }

    public DeclaredType(int startLine, int endLine, int startPos, int endPos, String name, T type) {
        super(startLine, endLine, startPos, endPos, name);
        this.type = type;
    }

    @Override
    public T getType() {
        return type;
    }

    @Override
    public String toString() {
        if (type instanceof AliasType) {
            return String.format("type %s %s", name, ((AliasType) type).getInner());
        }
        return "type " + type;
    }
}
