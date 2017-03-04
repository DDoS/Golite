package ca.sapon.golite.semantic.type;

/**
 *
 */
public abstract class IndexableType extends Type {
    protected final Type component;

    protected IndexableType(Type component) {
        this.component = component;
    }

    public Type getComponent() {
        return component;
    }
}
