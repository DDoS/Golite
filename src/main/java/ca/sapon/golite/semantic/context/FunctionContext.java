package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.Function;
import ca.sapon.golite.semantic.symbol.Symbol;

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
    public void declareSymbol(Symbol symbol) {
        if (symbol instanceof Function) {
            throw new IllegalStateException("Cannot declare a function in a block context");
        }
        super.declareSymbol(symbol);
    }
}
