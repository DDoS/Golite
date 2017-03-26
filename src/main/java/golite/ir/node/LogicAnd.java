package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class LogicAnd extends Expr<BasicType> {
    private final Expr<BasicType> left;
    private final Expr<BasicType> right;

    public LogicAnd(Expr<BasicType> left, Expr<BasicType> right) {
        super(BasicType.BOOL);
        if (left.getType() != BasicType.BOOL || right.getType() != BasicType.BOOL) {
            throw new IllegalArgumentException("Expected bool-typed expressions");
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
        visitor.visitLogicAnd(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("(");
        left.print(printer);
        printer.print(" && ");
        right.print(printer);
        printer.print(")");
    }
}
