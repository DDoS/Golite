package ca.sapon.golite.semantic.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        return resolveShadowed(parent::resolveType, types, name);
    }

    public Optional<Variable> resolveVariable(String name) {
        return resolveShadowed(parent::resolveVariable, variables, name);
    }

    public Optional<Function> resolveFunction(String name) {
        return resolveShadowed(parent::resolveFunction, functions, name);
    }

    protected boolean declareType(NamedType type) {
        return declareSymbol(types, type);
    }

    protected boolean declareVariable(Variable variable) {
        return declareSymbol(variables, variable);
    }

    protected boolean declareFunction(Function function) {
        return declareSymbol(functions, function);
    }

    private static <T> Optional<T> resolveShadowed(java.util.function.Function<String, Optional<T>> parentSearch,
                                                   Map<String, T> search, String name) {
        final T item = search.get(name);
        if (item != null) {
            return Optional.of(item);
        }
        return parentSearch == null ? null : parentSearch.apply(name);
    }

    private static <T extends Symbol> boolean declareSymbol(Map<String, T> search, T symbol) {
        if (search.containsKey(symbol.getName())) {
            return false;
        }
        search.put(symbol.getName(), symbol);
        return true;
    }
}
