package golite.ir.node;

import golite.semantic.type.Type;

/**
 *
 */
public abstract class Expr<T extends Type> implements IrNode {
    protected final T type;

    protected Expr(T type) {
        this.type = type;
    }

    public T getType() {
        return type;
    }
}
