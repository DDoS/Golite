package golite.semantic.symbol;

import golite.semantic.check.TypeCheckerException;
import golite.semantic.type.FunctionType;
import golite.semantic.type.Type;
import golite.util.SourcePositioned;

/**
 * The symbol generated by a variable declaration.
 */
public class Variable<T  extends Type> extends Symbol<T> {
    private final Variable<T> aliasedOriginal;
    private final T type;

    public Variable(SourcePositioned source, String name, T type) {
        this(source.getStartLine(), source.getEndLine(), source.getStartPos(), source.getEndPos(), name, type);
    }

    public Variable(int startLine, int endLine, int startPos, int endPos, String name, T type) {
        this(null, startLine, endLine, startPos, endPos, name, type);
    }

    private Variable(Variable<T> aliasedOriginal, int startLine, int endLine, int startPos, int endPos, String name, T type) {
        super(startLine, endLine, startPos, endPos, name);
        this.aliasedOriginal = aliasedOriginal;
        this.type = type;
        if (type instanceof FunctionType) {
            throw new TypeCheckerException(this, "Cannot have a variable with a function type");
        }
    }

    @Override
    public T getType() {
        return type;
    }

    public Variable<T> dealias() {
        if (aliasedOriginal != null) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final T resolvedType = (T) type.deepResolve();
        return new Variable<>(this, getStartLine(), getEndLine(), getStartPos(), getEndPos(), name, resolvedType);
    }

    @Override
    public String toString() {
        return String.format("var %s %s", name, type);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Variable<?>)) {
            return false;
        }
        final Variable<?> that = (Variable<?>) object;
        final Variable<?> a = this.aliasedOriginal != null ? this.aliasedOriginal : this;
        final Variable<?> b = that.aliasedOriginal != null ? that.aliasedOriginal : that;
        return a == b;
    }

    @Override
    public int hashCode() {
        return aliasedOriginal != null ? aliasedOriginal.hashCode() : super.hashCode();
    }
}
