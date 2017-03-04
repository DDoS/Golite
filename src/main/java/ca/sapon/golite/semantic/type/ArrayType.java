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
        return length == arrayType.length && component.equals(arrayType.component);
    }

    @Override
    public int hashCode() {
        int result = component.hashCode();
        result = 31 * result + length;
        return result;
    }
}
