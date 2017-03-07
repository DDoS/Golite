package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.DeclaredType;
import ca.sapon.golite.semantic.symbol.Symbol;
import ca.sapon.golite.semantic.symbol.Variable;
import ca.sapon.golite.semantic.type.BasicType;

/**
 *
 */
public final class UniverseContext extends Context {
    private static final Variable TRUE_VARIABLE = new Variable("true", BasicType.BOOL, true);
    private static final Variable FALSE_VARIABLE = new Variable("false", BasicType.BOOL, true);
    public static final UniverseContext INSTANCE = new UniverseContext();

    private UniverseContext() {
        super(null, 0);
        BasicType.ALL.forEach(type -> symbols.put(type.getName(), new DeclaredType(type.getName(), type)));
        symbols.put(TRUE_VARIABLE.getName(), TRUE_VARIABLE);
        symbols.put(FALSE_VARIABLE.getName(), FALSE_VARIABLE);
    }

    @Override
    public void declareSymbol(Symbol symbol) {
        throw new IllegalStateException("The universe context is unmodifiable");
    }

    @Override
    public String getSignature() {
        return "Universe";
    }
}
