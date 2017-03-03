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

    public Variable(int startLine, int endLine, int startPos, int endPos, String name, Type type) {
        super(startLine, endLine, startPos, endPos, name);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
