package golite.semantic.context;

import golite.semantic.symbol.Function;
import golite.semantic.symbol.Symbol;

/**
 * A context for code blocks, such a "if", "for" and "switch" statements.
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
    public void declareSymbol(Symbol<?> symbol) {
        if (symbol instanceof Function) {
            throw new IllegalStateException("Cannot declare a function in a block context");
        }
        super.declareSymbol(symbol);
    }

    @Override
    public String getSignature() {
        return "CodeBlock: " + kind.toString().toLowerCase();
    }

    public enum Kind {
        IF, ELSE, FOR, SWITCH, CASE, BLOCK
    }
}
