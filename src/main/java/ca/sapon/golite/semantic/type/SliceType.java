package ca.sapon.golite.semantic.type;

/**
 *
 */
public class SliceType implements Type {
    private final Type component;

    public SliceType(Type component) {
        this.component = component;
    }

    @Override
    public String toString() {
        return "[]" + component.toString();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof SliceType && component.equals(((SliceType) other).component);
    }

    @Override
    public int hashCode() {
        return component.hashCode();
    }
}
