package golite.semantic.type;

/**
 * An array type, such as {@code [16]int}.
 */
public class ArrayType extends IndexableType {
    private final int length;

    public ArrayType(Type component, int length) {
        super(component);
        this.length = length;
    }

    @Override
    public boolean isComparable() {
        return component.isComparable();
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
