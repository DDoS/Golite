package ca.sapon.golite.semantic.symbol;

import ca.sapon.golite.semantic.type.FunctionType;
import ca.sapon.golite.semantic.type.Type;
import ca.sapon.golite.util.SourcePositioned;

/**
 *
 */
public class Function extends Symbol {
    private final FunctionType type;

    public Function(String name, FunctionType type) {
        super(name);
        this.type = type;
    }

    public Function(SourcePositioned source, String name, FunctionType type) {
        this(source.getStartLine(), source.getEndLine(), source.getStartPos(), source.getEndPos(), name, type);
    }

    public Function(int startLine, int endLine, int startPos, int endPos, String name, FunctionType type) {
        super(startLine, endLine, startPos, endPos, name);
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "func " + name + type.signature();
    }
}
