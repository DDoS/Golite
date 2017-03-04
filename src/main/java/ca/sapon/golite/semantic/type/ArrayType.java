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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ArrayType)) {
            return false;
        }
        final ArrayType arrayType = (ArrayType) other;
        return component.equals(arrayType.component) && length == arrayType.length;
    }

    @Override
    public int hashCode() {
        int result = component.hashCode();
        result = 31 * result + Integer.hashCode(length);
        return result;
    }
}
