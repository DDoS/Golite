package ca.sapon.golite.semantic.symbol;

import ca.sapon.golite.semantic.type.Type;

/**
 *
 */
public class Variable extends Symbol {
    private final Type type;
    private boolean constant;

    public Variable(String name, Type type, boolean constant) {
        super(name);
        this.type = type;
        this.constant = constant;
    }

    public Variable(int startLine, int endLine, int startPos, int endPos, String name, Type type, boolean constant) {
        super(startLine, endLine, startPos, endPos, name);
        this.type = type;
        this.constant = constant;
    }

    public Type getType() {
        return type;
    }

    public boolean isConstant() {
        return constant;
    }
}
