package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class PrintBool implements Stmt {
    private final Expr value;

    public PrintBool(Expr value) {
        this.value = value;
        if (value.getType() != BasicType.BOOL) {
            throw new IllegalArgumentException("Expected a bool-typed expression");
        }
    }

    public Expr getValue() {
        return value;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitPrintBool(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("printBool(");
        value.print(printer);
        printer.print(")");
    }
}
