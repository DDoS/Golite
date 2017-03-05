package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.Function;
import ca.sapon.golite.semantic.symbol.Symbol;

/**
 *
 */
public class CodeBlockContext extends Context {
    private final Kind kind;

    public CodeBlockContext(Context parent, int id, Kind kind) {
        super(parent, id);
        this.kind = kind;
        if (!(parent instanceof FunctionContext) && !(parent instanceof CodeBlockContext)) {
            throw new IllegalArgumentException("The parent context should be a function or another code block");
        }
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
        IF, FOR, SWITCH, BLOCK, ELSE
    }
}
