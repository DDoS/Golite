package ca.sapon.golite.type;

/**
 *
 */
public final class BasicType implements Type {
    public static final BasicType INT = new BasicType("int");
    public static final BasicType FLOAT64 = new BasicType("float64");
    public static final BasicType BOOL = new BasicType("bool");
    public static final BasicType RUNE = new BasicType("rune");
    public static final BasicType STRING = new BasicType("string");
    private final String name;

    private BasicType(String name) {
        this.name = name;
    }

    @Override
    public boolean assignableTo(Type type) {
        // Can widen an integer to a float
        return this == INT && type == FLOAT64;
    }

    @Override
    public String toString() {
        return name;
    }
}
