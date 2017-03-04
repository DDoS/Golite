package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.Function;
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
        BasicType.ALL.forEach(type -> types.put(type.getName(), new NamedType(type.getName(), type)));
        variables.put(TRUE_VARIABLE.getName(), TRUE_VARIABLE);
        variables.put(FALSE_VARIABLE.getName(), FALSE_VARIABLE);
    }

    @Override
    public void declareType(NamedType type) {
        throw new IllegalStateException("The universe context is unmodifiable");
    }

    @Override
    public void declareVariable(Variable variable) {
        throw new IllegalStateException("The universe context is unmodifiable");
    }

    @Override
    public void declareFunction(Function function) {
        throw new IllegalStateException("The universe context is unmodifiable");
    }
}
