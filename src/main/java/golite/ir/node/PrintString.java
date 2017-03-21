package golite.ir.node;

import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class PrintString implements Stmt {
    private final Expr value;

    public PrintString(Expr value) {
        this.value = value;
        if (value.getType() != BasicType.STRING) {
            throw new IllegalArgumentException("Expected a string-typed expression");
        }
    }

    public Expr getValue() {
        return value;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitPrintString(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("printString(");
        value.print(printer);
        printer.print(")");
    }
}
