package golite.ir.node;

import golite.util.SourcePrinter;

/**
 *
 */
public class PrintInt extends Stmt {
    private final Expr value;

    public PrintInt(Expr value) {
        this.value = value;
        if (!value.getType().isInteger()) {
            throw new IllegalArgumentException("Expected an integer-typed expression");
        }
    }

    public Expr getValue() {
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
