package ca.sapon.golite.semantic.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.sapon.golite.semantic.check.TypeCheckerException;
import ca.sapon.golite.semantic.symbol.Symbol;

/**
 *
 */
public abstract class Context {
    private final Context parent;
    private final int id;
    protected final Map<String, Symbol> symbols = new HashMap<>();

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

    @Override
    public String toString() {
        return symbols.toString();
    }
}
