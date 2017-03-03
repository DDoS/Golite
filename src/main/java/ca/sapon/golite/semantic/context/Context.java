package ca.sapon.golite.semantic.context;

import java.util.HashMap;
import java.util.Map;

import ca.sapon.golite.semantic.type.Type;

/**
 *
 */
public abstract class Context {
    private final Context parent;
    private final Map<String, Type> types = new HashMap<>();

    public Context(Context parent) {
        this.parent = parent;
    }

    public void declareType(String name, Type type) {
        if (types.containsKey(name)) {
            throw new RuntimeException("");
        }
        types.put(name, type);
    }
}
