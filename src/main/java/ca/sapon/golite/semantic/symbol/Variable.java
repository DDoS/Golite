package ca.sapon.golite.semantic.symbol;

import ca.sapon.golite.semantic.type.Type;

/**
 *
 */
public class Variable extends Symbol {
    private final Type type;

    public Variable(String name, Type type) {
        super(name);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
