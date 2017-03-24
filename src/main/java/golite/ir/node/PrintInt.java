package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class PrintInt implements Stmt {
    private final Expr<BasicType> value;

    public PrintInt(Expr<BasicType> value) {
        this.value = value;
        if (!value.getType().isInteger()) {
            throw new IllegalArgumentException("Expected an integer-typed expression");
        }
    }

    public Expr<BasicType> getValue() {
        return value;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitPrintInt(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("printInt(");
        value.print(printer);
        printer.print(")");
    }
}
