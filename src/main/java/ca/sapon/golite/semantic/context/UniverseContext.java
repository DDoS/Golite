package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.NamedType;
import ca.sapon.golite.semantic.type.BasicType;

/**
 *
 */
public final class UniverseContext extends Context {
    public static final UniverseContext INSTANCE = new UniverseContext();

    private UniverseContext() {
        super(null);
        BasicType.ALL.forEach(type -> declareType(new NamedType(type.getName(), type)));
    }
}
