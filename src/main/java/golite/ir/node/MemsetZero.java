package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.semantic.type.IndexableType;
import golite.semantic.type.StructType;
import golite.semantic.type.Type;
import golite.util.SourcePrinter;

/**
 *
 */
public class MemsetZero implements Stmt {
    private final Expr value;

    public MemsetZero(Expr value) {
        final Type type = value.getType();
        if (type != BasicType.STRING && !(type instanceof IndexableType) && !(type instanceof StructType)) {
            throw new IllegalArgumentException("Cannot memset the type " + type.getClass());
        }
        this.value = value;
    }

    public Expr getValue() {
        return value;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitMemsetZero(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("memset(");
        value.print(printer);
        printer.print(", 0)");
    }
}
