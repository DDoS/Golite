package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class UnaArInt extends Expr<BasicType> {
    private final Expr<BasicType> inner;
    private final Op op;

    public UnaArInt(Expr<BasicType> inner, Op op) {
        super(inner.getType());
        if (!inner.getType().isInteger()) {
            throw new IllegalArgumentException("Expected an integer-typed expression");
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
        visitor.visitUnaArInt(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print(op.toString());
        inner.print(printer);
    }

    public enum Op {
        NOP("+"), NEG("-"), BIT_NEG("^");
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
