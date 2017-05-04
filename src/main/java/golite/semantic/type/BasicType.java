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

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A basic type, such as {@code int}.
 */
public final class BasicType extends Type {
    public static final BasicType INT = new BasicType("int");
    public static final BasicType FLOAT64 = new BasicType("float64");
    public static final BasicType BOOL = new BasicType("bool");
    public static final BasicType RUNE = new BasicType("rune");
    public static final BasicType STRING = new BasicType("string");
    public static final Set<BasicType> ALL = Collections.unmodifiableSet(Stream.of(
            INT, FLOAT64, BOOL, RUNE, STRING
    ).collect(Collectors.toSet()));
    private static final Set<BasicType> NUMERICS = Collections.unmodifiableSet(Stream.of(
            INT, FLOAT64, RUNE
    ).collect(Collectors.toSet()));
    private static final Set<BasicType> INTEGERS = Collections.unmodifiableSet(Stream.of(
            INT, RUNE
    ).collect(Collectors.toSet()));
    private static final Set<BasicType> CAST_TYPES = Collections.unmodifiableSet(Stream.of(
            INT, FLOAT64, BOOL, RUNE
    ).collect(Collectors.toSet()));
    private final String name;

    private BasicType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isComparable() {
        return true;
    }

    @Override
    public boolean isOrdered() {
        return this != BOOL;
    }

    @Override
    public boolean isNumeric() {
        return NUMERICS.contains(this);
    }

    @Override
    public boolean isInteger() {
        return INTEGERS.contains(this);
    }

    @Override
    public BasicType deepResolve() {
        return this;
    }

    public boolean canCastTo() {
        return CAST_TYPES.contains(this);
    }

    public boolean canCastFrom(Type type) {
        if (!canCastTo()) {
            throw new IllegalStateException("This isn't a cast-able type: " + this);
        }
        // The type being cast must also be a basic cast type
        if (!(type instanceof BasicType) || !CAST_TYPES.contains(type)) {
            return false;
        }
        // Identity is always allowed
        if (this.equals(type)) {
            return true;
        }
        // Can cast from int, float64 or rune to any other (in other words: exclude booleans)
        return !this.equals(BOOL) && !type.equals(BOOL);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
