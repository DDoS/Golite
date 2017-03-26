package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class PrintString implements Stmt {
    private final Expr<BasicType> value;

    public PrintString(Expr<BasicType> value) {
        this.value = value;
        if (value.getType() != BasicType.STRING) {
            throw new IllegalArgumentException("Expected a string-typed expression");
        }
    }

    public Expr<BasicType> getValue() {
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
