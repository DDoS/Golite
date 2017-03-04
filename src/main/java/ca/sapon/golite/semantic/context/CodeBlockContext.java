package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.Function;
import ca.sapon.golite.semantic.symbol.Symbol;

/**
 *
 */
public class CodeBlockContext extends Context {
    private final Kind kind;

    public CodeBlockContext(FunctionContext parent, Kind kind) {
        super(parent);
        this.kind = kind;
    }

    public CodeBlockContext(CodeBlockContext parent, Kind kind) {
        super(parent);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public void declareSymbol(Symbol symbol) {
        if (symbol instanceof Function) {
            throw new IllegalStateException("Cannot declare a function in a block context");
        }
        super.declareSymbol(symbol);
    }

    public enum Kind {
        IF, FOR, SWITCH
    }
}
