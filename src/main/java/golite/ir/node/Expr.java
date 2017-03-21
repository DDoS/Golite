package golite.ir.node;

import golite.semantic.type.Type;

/**
 *
 */
public abstract class Expr implements IrNode {
    protected final Type type;

    protected Expr(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
