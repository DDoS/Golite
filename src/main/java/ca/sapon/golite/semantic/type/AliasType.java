package ca.sapon.golite.semantic.type;

/**
 *
 */
public class AliasType extends Type {
    private final String name;
    private final Type inner;

    public AliasType(String name, Type inner) {
        this.name = name;
        this.inner = inner;
    }

    public String getName() {
        return name;
    }

    public Type getInner() {
        return inner;
    }

    @Override
    public Type resolve() {
        Type innerMost = inner;
        while (innerMost instanceof AliasType) {
            innerMost = ((AliasType) innerMost).getInner();
        }
        return innerMost;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof AliasType && name.equals(((AliasType) other).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
