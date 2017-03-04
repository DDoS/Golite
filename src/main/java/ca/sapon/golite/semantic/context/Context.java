package ca.sapon.golite.semantic.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.sapon.golite.semantic.check.TypeCheckerException;
import ca.sapon.golite.semantic.symbol.Function;
import ca.sapon.golite.semantic.symbol.NamedType;
import ca.sapon.golite.semantic.symbol.Symbol;
import ca.sapon.golite.semantic.symbol.Variable;

/**
 *
 */
public abstract class Context {
    private final Context parent;
    protected final Map<String, NamedType> types = new HashMap<>();
    protected final Map<String, Variable> variables = new HashMap<>();
    protected final Map<String, Function> functions = new HashMap<>();

    protected Context(Context parent) {
        this.parent = parent;
    }

    public Context getParent() {
        return parent;
    }

    public Optional<NamedType> resolveType(String name) {
        return resolveShadowed(parent == null ? null : parent::resolveType, types, name);
    }

    public Optional<Variable> resolveVariable(String name) {
        return resolveShadowed(parent == null ? null : parent::resolveVariable, variables, name);
    }

    public Optional<Function> resolveFunction(String name) {
        return resolveShadowed(parent == null ? null : parent::resolveFunction, functions, name);
    }

    public void declareType(NamedType type) {
        declareSymbol(types, type);
    }

    public void declareVariable(Variable variable) {
        declareSymbol(variables, variable);
    }

    public void declareFunction(Function function) {
        declareSymbol(functions, function);
    }

    private static <T> Optional<T> resolveShadowed(java.util.function.Function<String, Optional<T>> parentSearch,
                                                   Map<String, T> search, String name) {
        if (name.equals("_")) {
            throw new IllegalArgumentException("Cannot resolve the blank identifier");
        }
        final T item = search.get(name);
        if (item != null) {
            return Optional.of(item);
        }
        return parentSearch == null ? Optional.empty() : parentSearch.apply(name);
    }

    private static <T extends Symbol> void declareSymbol(Map<String, T> mapping, T symbol) {
        if (symbol.getName().equals("_")) {
            // Don't declare blank symbols
            return;
        }
        if (mapping.containsKey(symbol.getName())) {
            throw new TypeCheckerException(symbol, "Cannot redeclare symbol " + symbol.getName());
        }
        mapping.put(symbol.getName(), symbol);
    }
}
