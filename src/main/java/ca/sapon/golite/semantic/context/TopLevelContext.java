package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.Function;
import ca.sapon.golite.semantic.symbol.NamedType;
import ca.sapon.golite.semantic.symbol.Variable;

/**
 *
 */
public class TopLevelContext extends Context {
    public TopLevelContext() {
        super(UniverseContext.INSTANCE);
    }

    @Override
    public boolean declareType(NamedType type) {
        return super.declareType(type);
    }

    @Override
    public boolean declareVariable(Variable variable) {
        return super.declareVariable(variable);
    }

    @Override
    public boolean declareFunction(Function function) {
        return super.declareFunction(function);
    }
}
