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

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import golite.semantic.check.TypeCheckerException;
import golite.semantic.symbol.Function;
import golite.semantic.symbol.Symbol;
import golite.util.SourcePrinter;

/**
 * A context in which symbols can be declared.
 */
public abstract class Context {
    private final Context parent;
    private final int id;
    protected final Map<String, Symbol<?>> symbols = new LinkedHashMap<>();

    protected Context(Context parent, int id) {
        this.parent = parent;
        if (parent != null && id <= parent.id) {
            throw new IllegalArgumentException("ID must be greater than that of the parent");
        }
        this.id = id;
    }

    public Context getParent() {
        return parent;
    }

    public int getID() {
        return id;
    }

    public Optional<Symbol<?>> lookupSymbol(String name) {
        return lookupSymbol(name, true);
    }

    public Optional<Symbol<?>> lookupSymbol(String name, boolean recursive) {
        if (name.equals("_")) {
            throw new IllegalArgumentException("Cannot resolve the blank identifier");
        }
        final Symbol<?> symbol = symbols.get(name);
        if (symbol != null) {
            return Optional.of(symbol);
        }
        return recursive && parent != null ? parent.lookupSymbol(name) : Optional.empty();
    }

    public void declareSymbol(Symbol<?> symbol) {
        final String name = symbol.getName();
        if (name.equals("_")) {
            // Don't declare blank symbols
            return;
        }
        if (symbols.containsKey(name)) {
            throw new TypeCheckerException(symbol, "Cannot redeclare symbol " + name);
        }
        symbols.put(name, symbol);
    }

    public Optional<Function> getEnclosingFunction() {
        if (parent != null) {
            return parent.getEnclosingFunction();
        }
        return Optional.empty();
    }

    public abstract String getSignature();

    public void print(SourcePrinter printer) {
        printer.print("[").print(Integer.toString(id)).print("] ").print(getSignature()).newLine();
        printer.indent();
        symbols.values().forEach(symbol -> printer.print(symbol.toString()).newLine());
        printer.dedent();
    }

    public void printAll(SourcePrinter printer) {
        if (parent != null) {
            parent.printAll(printer);
            printer.print("--------------------").newLine();
        }
        print(printer);
    }

    @Override
    public String toString() {
        final StringWriter writer = new StringWriter();
        print(new SourcePrinter(writer));
        return writer.toString();
    }

    public String toStringAll() {
        final StringWriter writer = new StringWriter();
        printAll(new SourcePrinter(writer));
        return writer.toString();
    }
}
