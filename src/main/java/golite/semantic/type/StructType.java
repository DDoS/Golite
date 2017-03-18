package golite.semantic.type;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
* An struct type, such as {@code struct {x int; y int; z int; _ int;}}.
 */
public class StructType extends Type {
    private final List<Field> fields;

    public StructType(List<Field> fields) {
        this.fields = fields;
    }

    @Override
    public boolean isComparable() {
        return fields.stream().allMatch(field -> field.getType().isComparable());
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

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof StructType && fields.equals(((StructType) other).fields);
    }

    @Override
    public int hashCode() {
        return fields.hashCode();
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

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Field)) {
                return false;
            }
            final Field field = (Field) other;
            return name.equals(field.name) && type.equals(field.type);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + type.hashCode();
            return result;
        }
    }
}
