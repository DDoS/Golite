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
    private static final Set<BasicType> NUMERICS = Collections.unmodifiableSet(Stream.of(
            INT, FLOAT64, RUNE
    ).collect(Collectors.toSet()));
    private static final Set<BasicType> INTEGERS = Collections.unmodifiableSet(Stream.of(
            INT, RUNE
    ).collect(Collectors.toSet()));
    private static final Set<BasicType> CAST_TYPES = Collections.unmodifiableSet(Stream.of(
            INT, FLOAT64, BOOL, RUNE
    ).collect(Collectors.toSet()));
    private final String name;

    private BasicType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isNumeric() {
        return NUMERICS.contains(this);
    }

    public boolean isInteger() {
        return INTEGERS.contains(this);
    }

    public boolean canCastTo() {
        return CAST_TYPES.contains(this);
    }

    public boolean canCastFrom(Type type) {
        if (!canCastTo()) {
            throw new IllegalStateException("This isn't a cast-able type: " + this);
        }
        // The type being cast must also be a basic cast type
        if (!(type instanceof BasicType) || !CAST_TYPES.contains(type)) {
            return false;
        }
        // Identity is always allowed
        if (this.equals(type)) {
            return true;
        }
        // Can cast from int, float64 or rune to any other (in other words: exclude booleans)
        return !this.equals(BOOL) && !type.equals(BOOL);
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
