package golite.semantic.symbol;

import golite.semantic.type.Type;
import golite.util.SourcePositioned;

/**
 * The symbol generated by a variable declaration.
 */
public class Variable extends Symbol {
    private final Type type;
    private Variable dealiased = null;

    public Variable(String name, Type type) {
        super(name);
        this.type = type;
    }

    public Variable(SourcePositioned source, String name, Type type) {
        this(source.getStartLine(), source.getEndLine(), source.getStartPos(), source.getEndPos(), name, type);
    }

    public Variable(int startLine, int endLine, int startPos, int endPos, String name, Type type) {
        super(startLine, endLine, startPos, endPos, name);
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    public Variable dealias() {
        if (dealiased != null) {
            return dealiased;
        }
        final Type resolvedType = type.deepResolve();
        if (type != resolvedType) {
            dealiased = new Variable(getStartLine(), getEndLine(), getStartPos(), getEndPos(), name, resolvedType);
        } else {
            dealiased = this;
        }
        return dealiased;
    }

    @Override
    public String toString() {
        return String.format("var %s %s", name, type);
    }
}