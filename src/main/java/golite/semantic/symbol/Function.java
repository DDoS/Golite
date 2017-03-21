package golite.semantic.symbol;

import java.util.List;
import java.util.stream.Collectors;

import golite.semantic.type.FunctionType;
import golite.util.SourcePositioned;

/**
 * The symbol generated by a function declaration.
 */
public class Function extends Symbol {
    private final Function aliasedOriginal;
    private final FunctionType type;
    private final List<Variable> parameters;

    public Function(SourcePositioned source, String name, FunctionType type, List<Variable> parameters) {
        this(source.getStartLine(), source.getEndLine(), source.getStartPos(), source.getEndPos(), name, type, parameters);
    }

    public Function(int startLine, int endLine, int startPos, int endPos, String name, FunctionType type, List<Variable> parameters) {
        this(null, startLine, endLine, startPos, endPos, name, type, parameters);
    }

    private Function(Function aliasedOriginal, int startLine, int endLine, int startPos, int endPos,
                     String name, FunctionType type, List<Variable> parameters) {
        super(startLine, endLine, startPos, endPos, name);
        this.aliasedOriginal = aliasedOriginal;
        this.type = type;
        this.parameters = parameters;
    }

    @Override
    public FunctionType getType() {
        return type;
    }

    public List<Variable> getParameters() {
        return parameters;
    }

    public Function dealias() {
        if (aliasedOriginal != null) {
            return this;
        }
        final FunctionType resolvedType = type.deepResolve();
        final List<Variable> dealiasedParams = parameters.stream().map(Variable::dealias).collect(Collectors.toList());
        return new Function(this, getStartLine(), getEndLine(), getStartPos(), getEndPos(), name, resolvedType, dealiasedParams);
    }

    @Override
    public String toString() {
        return "func " + name + type.signature();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Function)) {
            return false;
        }
        final Function that = (Function) object;
        final Function a = this.aliasedOriginal != null ? this.aliasedOriginal : this;
        final Function b = that.aliasedOriginal != null ? that.aliasedOriginal : that;
        return a == b;
    }

    @Override
    public int hashCode() {
        return aliasedOriginal != null ? aliasedOriginal.hashCode() : super.hashCode();
    }
}
