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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import golite.semantic.symbol.Function;

/**
 * A function type, such as {@code func(a int) int}. Only used for {@link Function}.
 */
public class FunctionType extends Type {
    private final List<Parameter> parameters;
    private final Type returnType;

    public FunctionType(List<Parameter> parameters, Type returnType) {
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public Optional<Type> getReturnType() {
        return Optional.ofNullable(returnType);
    }

    @Override
    public FunctionType deepResolve() {
        final List<Parameter> deepParams = parameters.stream().map(Parameter::deepResolve).collect(Collectors.toList());
        return new FunctionType(deepParams, returnType != null ? returnType.deepResolve() : null);
    }

    public String signature() {
        final List<String> paramStrings = parameters.stream().map(Object::toString).collect(Collectors.toList());
        return String.format("(%s)%s", String.join(", ", paramStrings), returnType == null ? "" : " " + returnType);
    }

    @Override
    public String toString() {
        return "func" + signature();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FunctionType)) {
            return false;
        }
        final FunctionType functionType = (FunctionType) other;
        return parameters.equals(functionType.parameters) && Objects.equals(returnType, functionType.returnType);
    }

    @Override
    public int hashCode() {
        int result = parameters.hashCode();
        result = 31 * result + returnType.hashCode();
        return result;
    }

    public static class Parameter {
        private final String name;
        private final Type type;

        public Parameter(String name, Type type) {
            this.type = type;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public Parameter deepResolve() {
            return new Parameter(name, type.deepResolve());
        }

        @Override
        public String toString() {
            return String.format("%s %s", name, type);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Parameter)) {
                return false;
            }
            final Parameter parameter = (Parameter) other;
            return name.equals(parameter.name) && type.equals(parameter.type);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + type.hashCode();
            return result;
        }
    }
}
