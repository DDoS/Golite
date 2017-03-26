package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.ArrayType;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class CmpArray extends Expr<BasicType> {
    private final Expr<ArrayType> left;
    private final Expr<ArrayType> right;
    private final Op op;

    public CmpArray(Expr<ArrayType> left, Expr<ArrayType> right, Op op) {
        super(BasicType.BOOL);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public Expr<ArrayType> getLeft() {
        return left;
    }

    public Expr<ArrayType> getRight() {
        return right;
    }

    public Op getOp() {
        return op;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitCmpArray(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        left.print(printer);
        printer.print(" ").print(op.toString()).print(" ");
        right.print(printer);
    }

    public enum Op {
        EQ("==["), NEQ("!=[");
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
