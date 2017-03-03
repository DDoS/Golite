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
    public void declareType(NamedType type) {
        super.declareType(type);
    }

    @Override
    public void declareVariable(Variable variable) {
        super.declareVariable(variable);
    }

    @Override
    public void declareFunction(Function function) {
        super.declareFunction(function);
    }
}
