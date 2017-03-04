package ca.sapon.golite.semantic.type;

/**
 *
 */
public class ArrayType extends IndexableType {
    private final int length;

    public ArrayType(Type component, int length) {
        super(component);
        this.length = length;
    }

    @Override
    public String toString() {
        return String.format("[%d]%s", length, component);
    }
}
