package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.NamedType;
import ca.sapon.golite.semantic.symbol.Variable;
import ca.sapon.golite.semantic.type.BasicType;

/**
 *
 */
public final class UniverseContext extends Context {
    public static final UniverseContext INSTANCE = new UniverseContext();
    private static final Variable TRUE_VARIABLE = new Variable("true", BasicType.BOOL, true);
    private static final Variable FALSE_VARIABLE = new Variable("false", BasicType.BOOL, true);

    private UniverseContext() {
        super(null);
        BasicType.ALL.forEach(type -> declareType(new NamedType(type.getName(), type)));
        declareVariable(TRUE_VARIABLE);
        declareVariable(FALSE_VARIABLE);
    }
}
