package golite.ir.node;

import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class StringLit extends Expr {
    private String value;

    public StringLit(String value) {
        super(BasicType.STRING);
        this.value = value;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitStringLit(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print(value);
    }
}
