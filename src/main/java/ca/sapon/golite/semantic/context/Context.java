package ca.sapon.golite.semantic.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.sapon.golite.semantic.symbol.Function;
import ca.sapon.golite.semantic.symbol.Variable;
import ca.sapon.golite.semantic.type.Type;

/**
 *
 */
public abstract class Context {
    private final Context parent;
    protected final Map<String, Type> types = new HashMap<>();
    protected final Map<String, Variable> variables = new HashMap<>();
    protected final Map<String, Function> functions = new HashMap<>();

    protected Context(Context parent) {
        this.parent = parent;
    }

    public Context getParent() {
        return parent;
    }

    public Optional<Type> resolveType(String name) {
        return resolveShadowed(parent::resolveType, types, name);
    }

    public Optional<Variable> resolveVariable(String name) {
        return resolveShadowed(parent::resolveVariable, variables, name);
    }

    public Optional<Function> resolveFunction(String name) {
        return resolveShadowed(parent::resolveFunction, functions, name);
    }

    protected boolean declareType(String name, Type type) {
        return declareUnique(types, name, type);
    }

    protected boolean declareVariable(String name, Variable variable) {
        return declareUnique(variables, name, variable);
    }

    protected boolean declareFunction(String name, Function function) {
        return declareUnique(functions, name, function);
    }

    private static <T> Optional<T> resolveShadowed(java.util.function.Function<String, Optional<T>> parentSearch,
                                                   Map<String, T> search, String name) {
        final T item = search.get(name);
        if (item != null) {
            return Optional.of(item);
        }
        return parentSearch == null ? null : parentSearch.apply(name);
    }

    private static <T> boolean declareUnique(Map<String, T> search, String name, T item) {
        if (search.containsKey(name)) {
            return false;
        }
        search.put(name, item);
        return true;
    }
}
