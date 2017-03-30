package golite.ir.node;

import java.util.Optional;

import golite.ir.IrVisitor;
import golite.semantic.type.StructType;
import golite.semantic.type.StructType.Field;
import golite.semantic.type.Type;
import golite.util.SourcePrinter;

/**
 *
 */
public class Select extends Expr<Type> {
    private final Expr<StructType> value;
    private final String field;

    public Select(Expr<StructType> value, String field) {
        super(checkFieldType(value.getType(), field));
        this.value = value;
        this.field = field;
    }

    public Expr<StructType> getValue() {
        return value;
    }

    public String getField() {
        return field;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitSelect(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        value.print(printer);
        printer.print(".").print(field);
    }

    private static Type checkFieldType(StructType type, String field) {
        final Optional<Field> optField = type.getField(field);
        if (!optField.isPresent()) {
            throw new IllegalArgumentException("Field " + field + " does not exist");
        }
        return optField.get().getType();
    }
}