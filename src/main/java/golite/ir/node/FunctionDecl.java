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
import golite.semantic.symbol.Variable;
import golite.util.SourcePrinter;

/**
 *
 */
public class FunctionDecl implements IrNode {
    private final Function function;
    private final List<String> paramUniqueNames;
    private final List<Stmt> statements;
    private final boolean staticInit;

    public FunctionDecl(Function function, List<String> paramUniqueNames, List<Stmt> statements) {
        this(function, paramUniqueNames, statements, false);
    }

    public FunctionDecl(Function function, List<String> paramUniqueNames, List<Stmt> statements, boolean staticInit) {
        this.function = function;
        this.paramUniqueNames = paramUniqueNames;
        this.statements = statements;
        this.staticInit = staticInit;
    }

    public Function getFunction() {
        return function;
    }

    public List<String> getParamUniqueNames() {
        return paramUniqueNames;
    }

    public List<Stmt> getStatements() {
        return statements;
    }

    public boolean isMain() {
        return function.getName().equals("main");
    }

    public boolean isStaticInit() {
        return staticInit;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitFunctionDecl(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        if (staticInit) {
            printer.print("init ");
        } else {
            printer.print("func ").print(function.getName()).print("(");
            final List<Variable<?>> parameters = function.getParameters();
            for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
                printer.print(paramUniqueNames.get(i)).print(" ").print(parameters.get(i).getType().toString());
                if (i < parametersSize - 1) {
                    printer.print(", ");
                }
            }
            printer.print(") ");
        }
        printer.print("{").newLine().indent();
        statements.forEach(stmt -> {
            stmt.print(printer);
            printer.newLine();
        });
        printer.dedent().print("}");
    }
}
