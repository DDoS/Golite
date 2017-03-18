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
    protected final Map<String, Symbol> symbols = new LinkedHashMap<>();

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

    public Optional<Symbol> lookupSymbol(String name) {
        return lookupSymbol(name, true);
    }

    public Optional<Symbol> lookupSymbol(String name, boolean recursive) {
        if (name.equals("_")) {
            throw new IllegalArgumentException("Cannot resolve the blank identifier");
        }
        final Symbol symbol = symbols.get(name);
        if (symbol != null) {
            return Optional.of(symbol);
        }
        return recursive && parent != null ? parent.lookupSymbol(name) : Optional.empty();
    }

    public void declareSymbol(Symbol symbol) {
        if (symbol.getName().equals("_")) {
            // Don't declare blank symbols
            return;
        }
        if (symbols.containsKey(symbol.getName())) {
            throw new TypeCheckerException(symbol, "Cannot redeclare symbol " + symbol.getName());
        }
        symbols.put(symbol.getName(), symbol);
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
