package ca.sapon.golite.typecheck.type;

/**
 *
 */
public class ArrayType extends Type {
    private final Type component;
    private final int length;

    public ArrayType(Type component, int length) {
        this.component = component;
        this.length = length;
    }

    @Override
    public boolean assignableTo(Type type) {
        return false;
    }

    @Override
    public String toString() {
        return String.format("[%d]%s", length, component);
    }
}
