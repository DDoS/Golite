package golite.semantic.context;

import golite.semantic.symbol.DeclaredType;
import golite.semantic.symbol.Symbol;
import golite.semantic.symbol.Variable;
import golite.semantic.type.BasicType;

/**
 * The top-most context, enclosing all Golite code by default.
 */
public final class UniverseContext extends Context {
    public static final Variable<BasicType> TRUE_VARIABLE = new Variable<>(0, 0, 0, 0, "true", BasicType.BOOL);
    public static final Variable<BasicType> FALSE_VARIABLE = new Variable<>(0, 0, 0, 0, "false", BasicType.BOOL);
    public static final UniverseContext INSTANCE = new UniverseContext();

    private UniverseContext() {
        super(null, 0);
        BasicType.ALL.forEach(type -> symbols.put(type.getName(), new DeclaredType<>(type.getName(), type)));
        symbols.put(TRUE_VARIABLE.getName(), TRUE_VARIABLE);
        symbols.put(FALSE_VARIABLE.getName(), FALSE_VARIABLE);
    }

    @Override
    public void declareSymbol(Symbol<?> symbol) {
        throw new IllegalStateException("The universe context is unmodifiable");
    }

    @Override
    public String getSignature() {
        return "Universe";
    }
}
