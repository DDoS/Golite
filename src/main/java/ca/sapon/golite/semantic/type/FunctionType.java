package ca.sapon.golite.semantic.type;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 */
public class FunctionType implements Type {
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
    }
}
