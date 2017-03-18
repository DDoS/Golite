package golite.semantic.context;

import java.util.Optional;

import golite.semantic.symbol.Function;
import golite.semantic.symbol.Symbol;

/**
 * The context of the body of a function declaration.
 */
public class FunctionContext extends Context {
    private final Function function;

    public FunctionContext(TopLevelContext parent, int id, Function function) {
        super(parent, id);
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

    @Override
    public Optional<Function> getEnclosingFunction() {
        return Optional.of(getFunction());
    }

    @Override
    public String getSignature() {
        return "Function: " + function;
    }
}
