package ca.sapon.golite.semantic.type;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 */
public class StructType implements Type {
    private final List<Field> fields;

    public StructType(List<Field> fields) {
        this.fields = fields;
    }

    public Optional<Field> getField(String name) {
        if (name.equals("_")) {
            throw new IllegalArgumentException("Cannot access fields with the blank identifier");
        }
        return fields.stream().filter(field -> field.name.equals(name)).findAny();
    }

    @Override
    public String toString() {
        final List<String> fieldStrings = fields.stream().map(Object::toString).collect(Collectors.toList());
        return String.format("struct {%s}", String.join("; ", fieldStrings));
    }

    public static class Field {
        private final String name;
        private final Type type;

        public Field(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        @Override
        public String toString() {
            return name + " " + type;
        }
    }
}
