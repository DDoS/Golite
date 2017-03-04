package ca.sapon.golite.semantic.symbol;

import ca.sapon.golite.semantic.type.Type;
import ca.sapon.golite.util.SourcePositioned;

/**
 *
 */
public class DeclaredType extends Symbol {
    private final Type type;

    public DeclaredType(String name, Type type) {
        super(name);
        this.type = type;
    }

    public DeclaredType(SourcePositioned source, String name, Type type) {
        this(source.getStartLine(), source.getEndLine(), source.getStartPos(), source.getEndPos(), name, type);
    }

    public DeclaredType(int startLine, int endLine, int startPos, int endPos, String name, Type type) {
        super(startLine, endLine, startPos, endPos, name);
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("type %s %s", name, type);
    }
}
