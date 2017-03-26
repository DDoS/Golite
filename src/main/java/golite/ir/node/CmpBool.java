package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class CmpBool extends Expr<BasicType> {
    private final Expr<BasicType> left;
    private final Expr<BasicType> right;
    private final Op op;

    public CmpBool(Expr<BasicType> left, Expr<BasicType> right, Op op) {
        super(BasicType.BOOL);
        if (left.getType() != BasicType.BOOL || right.getType() != BasicType.BOOL) {
            throw new IllegalArgumentException("Expected bool-typed expressions");
        }
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public Expr<BasicType> getLeft() {
        return left;
    }

    public Expr<BasicType> getRight() {
        return right;
    }

    public Op getOp() {
        return op;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitCmpBool(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        left.print(printer);
        printer.print(" ").print(op.toString()).print(" ");
        right.print(printer);
    }

    public enum Op {
        EQ("==@"), NEQ("!=@");
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
