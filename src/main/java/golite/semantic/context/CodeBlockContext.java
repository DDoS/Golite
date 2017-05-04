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
package golite.semantic.context;

import golite.semantic.symbol.Function;
import golite.semantic.symbol.Symbol;

/**
 * A context for code blocks, such a "if", "for" and "switch" statements.
 */
public class CodeBlockContext extends Context {
    private final Kind kind;

    public CodeBlockContext(Context parent, int id, Kind kind) {
        super(parent, id);
        this.kind = kind;
        if (!(parent instanceof FunctionContext) && !(parent instanceof CodeBlockContext)) {
            throw new IllegalArgumentException("The parent context should be a function or another code block");
        }
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public void declareSymbol(Symbol<?> symbol) {
        if (symbol instanceof Function) {
            throw new IllegalStateException("Cannot declare a function in a block context");
        }
        super.declareSymbol(symbol);
    }

    @Override
    public String getSignature() {
        return "CodeBlock: " + kind.toString().toLowerCase();
    }

    public enum Kind {
        IF, ELSE, FOR, SWITCH, CASE, BLOCK
    }
}
