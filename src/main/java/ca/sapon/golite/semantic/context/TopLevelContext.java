package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.Function;
import ca.sapon.golite.semantic.symbol.Variable;
import ca.sapon.golite.semantic.type.Type;

/**
 *
 */
public class TopLevelContext extends Context {
    public TopLevelContext() {
        super(UniverseContext.INSTANCE);
    }

    @Override
    public boolean declareType(String name, Type type) {
        return super.declareType(name, type);
    }

    @Override
    public boolean declareVariable(String name, Variable variable) {
        return super.declareVariable(name, variable);
    }

    @Override
    public boolean declareFunction(String name, Function function) {
        return super.declareFunction(name, function);
    }
}
