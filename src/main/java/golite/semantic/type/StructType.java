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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An struct type, such as {@code struct {x int; y int; z int; _ int;}}.
 */
public class StructType extends Type {
    private final List<Field> fields;

    public StructType(List<Field> fields) {
        this.fields = fields;
    }

    @Override
    public boolean isComparable() {
        return fields.stream().allMatch(field -> field.getType().isComparable());
    }

    public Optional<Field> getField(String name) {
        if (name.equals("_")) {
            throw new IllegalArgumentException("Cannot access fields with the blank identifier");
        }
        return fields.stream().filter(field -> field.name.equals(name)).findAny();
    }

    public List<Field> getFields() {
        return fields;
    }

    public int fieldIndex(String name) {
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getName().equals(name)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public StructType deepResolve() {
        return new StructType(fields.stream().map(Field::deepResolve).collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        final List<String> fieldStrings = fields.stream().map(Object::toString).collect(Collectors.toList());
        return String.format("struct {%s}", String.join("; ", fieldStrings));
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof StructType && fields.equals(((StructType) other).fields);
    }

    @Override
    public int hashCode() {
        return fields.hashCode();
    }

    public static class Field {
        private final String name;
        private final Type type;

        public Field(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public Field deepResolve() {
            return new Field(name, type.deepResolve());
        }

        @Override
        public String toString() {
            return name + " " + type;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Field)) {
                return false;
            }
            final Field field = (Field) other;
            return name.equals(field.name) && type.equals(field.type);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + type.hashCode();
            return result;
        }
    }
}
