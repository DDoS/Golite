package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class UnaArFloat64 extends Expr<BasicType> {
    private final Expr<BasicType> inner;
    private final Op op;

    public UnaArFloat64(Expr<BasicType> inner, Op op) {
        super(BasicType.FLOAT64);
        if (inner.getType() != BasicType.FLOAT64) {
            throw new IllegalArgumentException("Expected a float64-typed expression");
        }
        this.inner = inner;
        this.op = op;
    }

    public Expr<BasicType> getInner() {
        return inner;
    }

    public Op getOp() {
        return op;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitUnaArFloat64(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print(op.toString());
        inner.print(printer);
    }

    public enum Op {
        NOP("+."), NEG("-.");
        private final String string;

        Op(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }
}
