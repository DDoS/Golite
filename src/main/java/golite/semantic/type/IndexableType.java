package golite.semantic.type;

/**
 * A type that can be used with the indexing operation, such as an array or slice.
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
