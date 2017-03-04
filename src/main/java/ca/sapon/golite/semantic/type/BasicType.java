package ca.sapon.golite.semantic.type;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public final class BasicType extends Type {
    public static final BasicType INT = new BasicType("int");
    public static final BasicType FLOAT64 = new BasicType("float64");
    public static final BasicType BOOL = new BasicType("bool");
    public static final BasicType RUNE = new BasicType("rune");
    public static final BasicType STRING = new BasicType("string");
    public static final Set<BasicType> ALL = Collections.unmodifiableSet(Stream.of(
            INT, FLOAT64, BOOL, RUNE, STRING
    ).collect(Collectors.toSet()));
    public static final Set<BasicType> CAST_TYPES = Collections.unmodifiableSet(Stream.of(
            INT, FLOAT64, BOOL, RUNE
    ).collect(Collectors.toSet()));
    private final String name;

    private BasicType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean canCastFrom(Type type) {
        if (!CAST_TYPES.contains(this)) {
            throw new IllegalStateException("This isn't a cast-able type: " + this);
        }
        // Identity is allowed
        if (this.equals(type)) {
            return true;
        }
        // Int or rune to float (and vice-versa)
        if ((this.equals(BasicType.INT) || this.equals(BasicType.RUNE)) && type.equals(BasicType.FLOAT64)
                || this.equals(BasicType.FLOAT64) && (type.equals(BasicType.INT) || type.equals(BasicType.RUNE))) {
            return true;
        }
        // Int to rune (and vice-versa)
        return this.equals(BasicType.INT) && type.equals(BasicType.RUNE)
                || this.equals(BasicType.RUNE) && type.equals(BasicType.INT);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
