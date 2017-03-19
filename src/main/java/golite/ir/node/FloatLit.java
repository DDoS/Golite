package golite.ir.node;

import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class FloatLit extends Expr {
    private final double value;

    public FloatLit(double value) {
        super(BasicType.FLOAT64);
        this.value = value;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitFloatLit(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print(Double.toString(value));
    }
}
