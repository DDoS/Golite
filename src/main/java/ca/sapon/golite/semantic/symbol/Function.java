package ca.sapon.golite.semantic.symbol;

import java.util.List;
import java.util.Optional;

import ca.sapon.golite.semantic.type.Type;

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
}
