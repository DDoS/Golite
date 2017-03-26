package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.semantic.type.StructType;
import golite.util.SourcePrinter;

/**
 *
 */
public class CmpStruct extends Expr<BasicType> {
    private final Expr<StructType> left;
    private final Expr<StructType> right;
    private final Op op;

    public CmpStruct(Expr<StructType> left, Expr<StructType> right, Op op) {
        super(BasicType.BOOL);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public Expr<StructType> getLeft() {
        return left;
    }

    public Expr<StructType> getRight() {
        return right;
    }

    public Op getOp() {
        return op;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitCmpStruct(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        left.print(printer);
        printer.print(" ").print(op.toString()).print(" ");
        right.print(printer);
    }

    public enum Op {
        EQ("==*"), NEQ("!=*");
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
