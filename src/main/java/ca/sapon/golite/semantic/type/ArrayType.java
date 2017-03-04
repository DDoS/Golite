package ca.sapon.golite.semantic.type;

/**
 *
 */
public class ArrayType implements Type {
    private final Type component;
    private final int length;

    public ArrayType(Type component, int length) {
        this.component = component;
        this.length = length;
    }

    @Override
    public boolean assignableTo(Type type) {
        // The types must be the same
        return this == type;
    }

    @Override
    public String toString() {
        return String.format("[%d]%s", length, component);
    }
}
