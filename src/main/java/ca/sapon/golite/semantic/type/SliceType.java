package ca.sapon.golite.semantic.type;

/**
 *
 */
public class SliceType extends IndexableType {
    public SliceType(Type component) {
        super(component);
    }

    @Override
    public String toString() {
        return "[]" + component.toString();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ArrayType && component.equals(((ArrayType) other).component);
    }

    @Override
    public int hashCode() {
        return component.hashCode();
    }
}
