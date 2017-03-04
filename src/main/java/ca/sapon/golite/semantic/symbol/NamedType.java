package ca.sapon.golite.semantic.symbol;

import ca.sapon.golite.semantic.type.Type;
import ca.sapon.golite.util.SourcePositioned;

/**
 *
 */
public class NamedType extends Symbol {
    private final Type type;

    public NamedType(String name, Type type) {
        super(name);
        this.type = type;
    }

    public NamedType(SourcePositioned source, String name, Type type) {
        this(source.getStartLine(), source.getEndLine(), source.getStartPos(), source.getEndPos(), name, type);
    }

    public NamedType(int startLine, int endLine, int startPos, int endPos, String name, Type type) {
        super(startLine, endLine, startPos, endPos, name);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
