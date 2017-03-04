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
}
