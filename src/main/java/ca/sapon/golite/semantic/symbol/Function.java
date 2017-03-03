package ca.sapon.golite.semantic.symbol;

import java.util.Optional;

import ca.sapon.golite.semantic.type.Type;

/**
 *
 */
public class Function extends Symbol {
    private final Variable[] parameters;
    private final Type type;

    public Function(String name, Type type, Variable... parameters) {
        super(name);
        this.type = type;
        this.parameters = parameters;
    }

    public Function(int startLine, int endLine, int startPos, int endPos, String name, Type type, Variable... parameters) {
        super(startLine, endLine, startPos, endPos, name);
        this.type = type;
        this.parameters = parameters;
    }

    public Variable[] getParameters() {
        return parameters;
    }

    public Optional<Type> getType() {
        return Optional.ofNullable(type);
    }
}
