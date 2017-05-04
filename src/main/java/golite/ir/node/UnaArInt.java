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

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class UnaArInt extends Expr<BasicType> {
    private final Expr<BasicType> inner;
    private final Op op;

    public UnaArInt(Expr<BasicType> inner, Op op) {
        super(inner.getType());
        if (!inner.getType().isInteger()) {
            throw new IllegalArgumentException("Expected an integer-typed expression");
        }
        this.inner = inner;
        this.op = op;
    }

    public Expr<BasicType> getInner() {
        return inner;
    }

    public Op getOp() {
        return op;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitUnaArInt(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print(op.toString());
        inner.print(printer);
    }

    public enum Op {
        NOP("+"), NEG("-"), BIT_NEG("^");
        private final String string;

        Op(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }
}
