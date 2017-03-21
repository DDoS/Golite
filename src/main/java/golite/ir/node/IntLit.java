package golite.ir.node;

import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class IntLit extends Expr {
    private final int value;

    public IntLit(int value) {
        super(BasicType.INT);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitIntLit(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print(Integer.toString(value));
    }
}
