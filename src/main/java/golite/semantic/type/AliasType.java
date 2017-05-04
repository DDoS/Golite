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
 * A named type that refers to another type. It could also be an alias type.
 */
public class AliasType extends Type {
    private final int contextID;
    private final String name;
    private final Type inner;

    public AliasType(int contextID, String name, Type inner) {
        this.contextID = contextID;
        this.name = name;
        this.inner = inner;
    }

    public String getName() {
        return name;
    }

    public Type getInner() {
        return inner;
    }

    @Override
    public boolean isNumeric() {
        return inner.isNumeric();
    }

    @Override
    public boolean isInteger() {
        return inner.isInteger();
    }

    @Override
    public boolean isComparable() {
        return inner.isComparable();
    }

    @Override
    public boolean isOrdered() {
        return inner.isOrdered();
    }

    @Override
    public Type resolve() {
        Type innerMost = inner;
        while (innerMost instanceof AliasType) {
            innerMost = ((AliasType) innerMost).getInner();
        }
        return innerMost;
    }

    @Override
    public Type deepResolve() {
        return resolve().deepResolve();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AliasType)) {
            return false;
        }
        final AliasType aliasType = (AliasType) other;
        return contextID == aliasType.contextID && name.equals((aliasType).name);
    }

    @Override
    public int hashCode() {
        int result = contextID;
        result = 31 * result + name.hashCode();
        return result;
    }
}
