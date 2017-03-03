package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.NamedType;
import ca.sapon.golite.semantic.symbol.Variable;

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
    public void declareType(NamedType type) {
        super.declareType(type);
    }

    @Override
    public void declareVariable(Variable variable) {
        super.declareVariable(variable);
    }

    public enum Kind {
        IF, FOR, SWITCH
    }
}
