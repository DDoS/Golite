package golite.ir.node;

import golite.ir.IrVisitor;
import golite.util.SourcePrinter;

/**
 *
 */
public class ValueReturn implements Stmt {
    private final Expr value;

    public ValueReturn(Expr value) {
        this.value = value;
    }

    public Expr getValue() {
        return value;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitValueReturn(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("return ");
        value.print(printer);
    }
}
