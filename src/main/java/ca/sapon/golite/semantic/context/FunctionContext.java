package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.NamedType;
import ca.sapon.golite.semantic.symbol.Variable;

/**
 *
 */
public class FunctionContext extends Context {
    public FunctionContext(TopLevelContext parent) {
        super(parent);
    }

    @Override
    public boolean declareType(NamedType type) {
        return super.declareType(type);
    }

    @Override
    public boolean declareVariable(Variable variable) {
        return super.declareVariable(variable);
    }
}
