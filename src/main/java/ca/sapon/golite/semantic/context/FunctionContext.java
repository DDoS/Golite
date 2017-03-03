package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.Variable;
import ca.sapon.golite.semantic.type.Type;

/**
 *
 */
public class FunctionContext extends Context {
    public FunctionContext(TopLevelContext parent) {
        super(parent);
    }

    @Override
    public boolean declareType(String name, Type type) {
        return super.declareType(name, type);
    }

    @Override
    public boolean declareVariable(String name, Variable variable) {
        return super.declareVariable(name, variable);
    }
}
