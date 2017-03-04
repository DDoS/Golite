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
    protected final Map<String, Symbol> symbols = new HashMap<>();

    protected Context(Context parent) {
        this.parent = parent;
    }

    public Context getParent() {
        return parent;
    }

    public Optional<Symbol> resolveSymbol(String name) {
        if (name.equals("_")) {
            throw new IllegalArgumentException("Cannot resolve the blank identifier");
        }
        final Symbol symbol = symbols.get(name);
        if (symbol != null) {
            return Optional.of(symbol);
        }
        return parent == null ? Optional.empty() : parent.resolveSymbol(name);
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
