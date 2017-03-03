package ca.sapon.golite.typecheck.context;

import java.util.HashMap;
import java.util.Map;

import ca.sapon.golite.typecheck.type.Type;

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
