package ca.sapon.golite.semantic.type;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 */
public class FunctionType extends Type {
    private final List<Parameter> parameters;
    private final Type returnType;

    public FunctionType(List<Parameter> parameters, Type returnType) {
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public Optional<Type> getReturnType() {
        return Optional.ofNullable(returnType);
    }

    public String signature() {
        final List<String> paramStrings = parameters.stream().map(Object::toString).collect(Collectors.toList());
        return String.format("(%s)%s", String.join(", ", paramStrings), returnType == null ? "" : " " + returnType);
    }

    @Override
    public String toString() {
        return "func" + signature();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FunctionType)) {
            return false;
        }
        final FunctionType functionType = (FunctionType) other;
        return parameters.equals(functionType.parameters) && returnType.equals(functionType.returnType);
    }

    @Override
    public int hashCode() {
        int result = parameters.hashCode();
        result = 31 * result + returnType.hashCode();
        return result;
    }

    public static class Parameter {
        private final String name;
        private final Type type;

        public Parameter(String name, Type type) {
            this.type = type;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        @Override
        public String toString() {
            return String.format("%s %s", name, type);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Parameter)) {
                return false;
            }
            final Parameter parameter = (Parameter) other;
            return name.equals(parameter.name) && type.equals(parameter.type);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + type.hashCode();
            return result;
        }
    }
}
