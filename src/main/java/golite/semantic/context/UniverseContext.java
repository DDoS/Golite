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

import golite.semantic.symbol.DeclaredType;
import golite.semantic.symbol.Symbol;
import golite.semantic.symbol.Variable;
import golite.semantic.type.BasicType;

/**
 * The top-most context, enclosing all Golite code by default.
 */
public final class UniverseContext extends Context {
    public static final Variable<BasicType> TRUE_VARIABLE = new Variable<>(0, 0, 0, 0, "true", BasicType.BOOL);
    public static final Variable<BasicType> FALSE_VARIABLE = new Variable<>(0, 0, 0, 0, "false", BasicType.BOOL);
    public static final UniverseContext INSTANCE = new UniverseContext();

    private UniverseContext() {
        super(null, 0);
        BasicType.ALL.forEach(type -> symbols.put(type.getName(), new DeclaredType<>(type.getName(), type)));
        symbols.put(TRUE_VARIABLE.getName(), TRUE_VARIABLE);
        symbols.put(FALSE_VARIABLE.getName(), FALSE_VARIABLE);
    }

    @Override
    public void declareSymbol(Symbol<?> symbol) {
        throw new IllegalStateException("The universe context is unmodifiable");
    }

    @Override
    public String getSignature() {
        return "Universe";
    }
}
