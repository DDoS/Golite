package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class ConcatString extends Expr<BasicType> {
    private final Expr<BasicType> left;
    private final Expr<BasicType> right;

    public ConcatString(Expr<BasicType> left, Expr<BasicType> right) {
        super(BasicType.STRING);
        if (left.getType() != BasicType.STRING || right.getType() != BasicType.STRING) {
            throw new IllegalArgumentException("Expected string-typed expressions");
        }
        this.left = left;
        this.right = right;
    }

    public Expr<BasicType> getLeft() {
        return left;
    }

    public Expr<BasicType> getRight() {
        return right;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitConcatString(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("(");
        left.print(printer);
        printer.print(" ~ ");
        right.print(printer);
        printer.print(")");
    }
}
