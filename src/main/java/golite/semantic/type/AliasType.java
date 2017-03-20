package golite.semantic.type;

/**
 * A named type that refers to another type. It could also be an alias type.
 */
public class AliasType extends Type {
    private final int contextID;
    private final String name;
    private final Type inner;

    public AliasType(int contextID, String name, Type inner) {
        this.contextID = contextID;
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
    public Type deepResolve() {
        return resolve().deepResolve();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AliasType)) {
            return false;
        }
        final AliasType aliasType = (AliasType) other;
        return contextID == aliasType.contextID && name.equals((aliasType).name);
    }

    @Override
    public int hashCode() {
        int result = contextID;
        result = 31 * result + name.hashCode();
        return result;
    }
}
