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

import java.util.List;

import golite.ir.IrVisitor;
import golite.semantic.symbol.Function;
import golite.semantic.type.Type;
import golite.util.SourcePrinter;

/**
 *
 */
public class Call extends Expr<Type> implements Stmt {
    private final Function function;
    private final List<Expr<?>> arguments;

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public Call(Function function, List<Expr<?>> arguments) {
        super(function.getType().getReturnType().orElse(null));
        this.function = function;
        this.arguments = arguments;
    }

    public Function getFunction() {
        return function;
    }

    public List<Expr<?>> getArguments() {
        return arguments;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitCall(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("call(").print(function.getName());
        for (Expr<?> argument : arguments) {
            printer.print(", ");
            argument.print(printer);
        }
        printer.print(")");
    }
}
