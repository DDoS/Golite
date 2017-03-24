package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class Float64Lit extends Expr {
    private final double value;

    public Float64Lit(double value) {
        super(BasicType.FLOAT64);
        this.value = value;
    }

    public double getValue() {
        return value;
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
