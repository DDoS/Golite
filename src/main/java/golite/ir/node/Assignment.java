package golite.ir.node;

import golite.ir.IrVisitor;
import golite.util.SourcePrinter;

/**
 *
 */
public class Assignment implements Stmt {
    private final Expr left;
    private final Expr right;

    public Assignment(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public Expr getLeft() {
        return left;
    }

    public Expr getRight() {
        return right;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitAssignment(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("assign(");
        left.print(printer);
        printer.print(", ");
        right.print(printer);
        printer.print(")");
    }
}
