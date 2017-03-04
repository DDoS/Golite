package ca.sapon.golite.semantic.type;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public final class BasicType implements Type {
    public static final BasicType INT = new BasicType("int");
    public static final BasicType FLOAT64 = new BasicType("float64");
    public static final BasicType BOOL = new BasicType("bool");
    public static final BasicType RUNE = new BasicType("rune");
    public static final BasicType STRING = new BasicType("string");
    public static final Set<BasicType> ALL = Collections.unmodifiableSet(Stream.of(
            INT, FLOAT64, BOOL, RUNE, STRING
    ).collect(Collectors.toSet()));
    private final String name;

    private BasicType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean assignableTo(Type type) {
        // The types must be the same
        return this == type;
    }

    @Override
    public String toString() {
        return name;
    }
}
