package ca.sapon.golite.semantic.context;

import ca.sapon.golite.semantic.symbol.Variable;
import ca.sapon.golite.semantic.type.Type;

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
    public boolean declareType(String name, Type type) {
        return super.declareType(name, type);
    }

    @Override
    public boolean declareVariable(String name, Variable variable) {
        return super.declareVariable(name, variable);
    }

    public enum Kind {
        IF, FOR, SWITCH
    }
}
