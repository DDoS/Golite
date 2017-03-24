package golite.ir.node;

import golite.ir.IrVisitor;
import golite.util.SourcePrinter;

/**
 *
 */
public class PrintRune implements Stmt {
    private final Expr value;

    public PrintRune(Expr value) {
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
        visitor.visitPrintRune(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("printRune(");
        value.print(printer);
        printer.print(")");
    }
}
