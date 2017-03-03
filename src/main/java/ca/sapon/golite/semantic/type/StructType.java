package ca.sapon.golite.semantic.type;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class StructType implements Type {
    private final Field[] fields;

    public StructType(Field[] fields) {
        this.fields = fields;
    }

    @Override
    public boolean assignableTo(Type type) {
        return false;
    }

    @Override
    public String toString() {
        final List<String> fieldStrings = Stream.of(fields).map(Object::toString).collect(Collectors.toList());
        return String.format("struct {%s}", String.join("; ", fieldStrings));
    }

    public static class Field {
        private final String name;
        private final Type type;

        public Field(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return name + " " + type;
        }
    }
}
