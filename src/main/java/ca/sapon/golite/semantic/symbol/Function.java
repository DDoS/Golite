package ca.sapon.golite.semantic.symbol;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ca.sapon.golite.semantic.type.Type;
import ca.sapon.golite.util.SourcePositioned;

/**
 *
 */
public class Function extends Symbol {
    private final List<Variable> parameters;
    private final Type type;

    public Function(String name, List<Variable> parameters, Type type) {
        super(name);
        this.parameters = parameters;
        this.type = type;
    }

    public Function(SourcePositioned source, String name, List<Variable> parameters, Type type) {
        this(source.getStartLine(), source.getEndLine(), source.getStartPos(), source.getEndPos(), name, parameters, type);
    }

    public Function(int startLine, int endLine, int startPos, int endPos, String name, List<Variable> parameters, Type type) {
        super(startLine, endLine, startPos, endPos, name);
        this.parameters = parameters;
        this.type = type;
    }

    public List<Variable> getParameters() {
        return parameters;
    }

    public Optional<Type> getType() {
        return Optional.ofNullable(type);
    }

    @Override
    public String toString() {
        final List<String> paramStrings = parameters.stream().map(Function::paramToString).collect(Collectors.toList());
        return String.format("func %s(%s)%s", name, String.join(", ", paramStrings), type == null ? "" : " " + type);
    }

    private static String paramToString(Variable param) {
        return String.format("%s %s", param.getName(), param.getType());
    }
}
