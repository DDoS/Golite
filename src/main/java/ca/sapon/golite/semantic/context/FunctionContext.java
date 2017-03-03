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
    public void declareType(NamedType type) {
        super.declareType(type);
    }

    @Override
    public void declareVariable(Variable variable) {
        super.declareVariable(variable);
    }
}
