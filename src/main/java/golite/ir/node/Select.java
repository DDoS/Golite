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

import java.util.Optional;

import golite.ir.IrVisitor;
import golite.semantic.type.StructType;
import golite.semantic.type.StructType.Field;
import golite.semantic.type.Type;
import golite.util.SourcePrinter;

/**
 *
 */
public class Select extends Expr<Type> {
    private final Expr<StructType> value;
    private final String field;

    public Select(Expr<StructType> value, String field) {
        super(checkFieldType(value.getType(), field));
        this.value = value;
        this.field = field;
    }

    public Expr<StructType> getValue() {
        return value;
    }

    public String getField() {
        return field;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitSelect(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        value.print(printer);
        printer.print(".").print(field);
    }

    private static Type checkFieldType(StructType type, String field) {
        final Optional<Field> optField = type.getField(field);
        if (!optField.isPresent()) {
            throw new IllegalArgumentException("Field " + field + " does not exist");
        }
        return optField.get().getType();
    }
}
