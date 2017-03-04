package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.Function;

/**
 *
 */
public class FunctionContext extends Context {
    public FunctionContext(TopLevelContext parent) {
        super(parent);
    }

    @Override
    public void declareFunction(Function function) {
        throw new IllegalStateException("Cannot declare a function in a function context");
    }
}
