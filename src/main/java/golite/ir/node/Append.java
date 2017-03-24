package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.SliceType;
import golite.util.SourcePrinter;

/**
 *
 */
public class Append extends Expr<SliceType> {
    private final Expr<SliceType> left;
    private final Expr<?> right;

    public Append(Expr<SliceType> left, Expr<?> right) {
        super(left.getType());
        this.left = left;
        this.right = right;
    }

    public Expr<SliceType> getLeft() {
        return left;
    }

    public Expr<?> getRight() {
        return right;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitAppend(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("append(");
        left.print(printer);
        printer.print(", ");
        right.print(printer);
        printer.print(")");
    }
}
