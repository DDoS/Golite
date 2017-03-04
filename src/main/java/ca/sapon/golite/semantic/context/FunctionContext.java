package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.Function;

/**
 *
 */
public class FunctionContext extends Context {
    private final Function function;

    public FunctionContext(TopLevelContext parent, Function function) {
        super(parent);
        this.function = function;
    }

    public Function getFunction() {
        return function;
    }

    @Override
    public void declareFunction(Function function) {
        throw new IllegalStateException("Cannot declare a function in a function context");
    }
}
