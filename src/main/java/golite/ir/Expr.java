package golite.ir;

import golite.semantic.type.Type;

/**
 *
 */
public abstract class Expr extends IrNode {
    protected final Type type;

    protected Expr(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
