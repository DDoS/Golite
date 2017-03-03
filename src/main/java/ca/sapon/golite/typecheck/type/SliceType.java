package ca.sapon.golite.typecheck.type;

/**
 *
 */
public class SliceType implements Type {
    private final Type component;

    public SliceType(Type component) {
        this.component = component;
    }

    @Override
    public boolean assignableTo(Type type) {
        return false;
    }

    @Override
    public String toString() {
        return "[]" + component.toString();
    }
}
