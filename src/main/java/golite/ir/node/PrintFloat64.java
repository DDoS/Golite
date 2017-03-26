package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class PrintFloat64 implements Stmt {
    private final Expr<BasicType> value;

    public PrintFloat64(Expr<BasicType> value) {
        this.value = value;
        if (value.getType() != BasicType.FLOAT64) {
            throw new IllegalArgumentException("Expected a float64-typed expression");
        }
    }

    public Expr<BasicType> getValue() {
        return value;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitPrintFloat64(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("printFloat64(");
        value.print(printer);
        printer.print(")");
    }
}
