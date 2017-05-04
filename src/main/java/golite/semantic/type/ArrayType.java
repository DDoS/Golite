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
package golite.semantic.type;

/**
 * An array type, such as {@code [16]int}.
 */
public class ArrayType extends IndexableType {
    private final int length;

    public ArrayType(Type component, int length) {
        super(component);
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    @Override
    public boolean isComparable() {
        return component.isComparable();
    }

    @Override
    public ArrayType deepResolve() {
        return new ArrayType(component.deepResolve(), length);
    }

    @Override
    public String toString() {
        return String.format("[%d]%s", length, component);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ArrayType)) {
            return false;
        }
        final ArrayType arrayType = (ArrayType) other;
        return component.equals(arrayType.component) && length == arrayType.length;
    }

    @Override
    public int hashCode() {
        int result = component.hashCode();
        result = 31 * result + Integer.hashCode(length);
        return result;
    }
}
