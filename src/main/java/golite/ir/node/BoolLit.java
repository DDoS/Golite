package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class BoolLit extends Expr<BasicType> {
    private final boolean value;

    public BoolLit(boolean value) {
        super(BasicType.BOOL);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitBoolLit(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print(Boolean.toString(value));
    }
}
