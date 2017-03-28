package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class CmpString extends Expr<BasicType> {
    private final Expr<BasicType> left;
    private final Expr<BasicType> right;
    private final Op op;

    public CmpString(Expr<BasicType> left, Expr<BasicType> right, Op op) {
        super(BasicType.BOOL);
        if (left.getType() != BasicType.STRING || right.getType() != BasicType.STRING) {
            throw new IllegalArgumentException("Expected string-typed expressions");
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
        visitor.visitCmpString(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("(");
        left.print(printer);
        printer.print(" ").print(op.toString()).print(" ");
        right.print(printer);
        printer.print(")");
    }

    public enum Op {
        EQ("==$", 0), NEQ("!=$", 1), LESS("<$", 2), LESS_EQ("<=$", 3), GREAT(">$", 4), GREAT_EQ(">=$", 5);
        private final String string;
        private final int runtimeID;

        Op(String string, int runtimeID) {
            this.string = string;
            this.runtimeID = runtimeID;
        }

        public int getRuntimeID() {
            return runtimeID;
        }

        @Override
        public String toString() {
            return string;
        }
    }
}
