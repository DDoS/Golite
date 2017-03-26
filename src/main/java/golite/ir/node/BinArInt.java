package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class BinArInt extends Expr<BasicType> {
    private final Expr<BasicType> left;
    private final Expr<BasicType> right;
    private final Op op;

    public BinArInt(Expr<BasicType> left, Expr<BasicType> right, Op op) {
        super(BasicType.INT);
        if (!left.getType().isInteger() || !right.getType().isInteger()) {
            throw new IllegalArgumentException("Expected integer-typed expressions");
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
        visitor.visitBinArInt(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        left.print(printer);
        printer.print(" ").print(op.toString()).print(" ");
        right.print(printer);
    }

    public enum Op {
        ADD("+"), SUB("-"), MUL("*"), DIV("/"), REM("%"), BIT_OR("|"), BIT_AND("&"),
        LSHIFT("<<"), RSHIFT(">>"), BIT_AND_NOT("&^"), BIT_XOR("^");
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
