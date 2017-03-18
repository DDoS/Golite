package golite.semantic.type;

/**
 * A Golite type.
 */
public abstract class Type {
    public boolean isNumeric() {
        return false;
    }

    public boolean isInteger() {
        return false;
    }

    public boolean isComparable() {
        return false;
    }

    public boolean isOrdered() {
        return false;
    }

    public Type resolve() {
        return this;
    }
}
