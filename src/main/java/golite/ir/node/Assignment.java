package golite.ir.node;

import golite.util.SourcePrinter;

/**
 *
 */
public class Assignment implements Stmt {
    private final Expr right;
    private final Expr left;

    public Assignment(Expr right, Expr left) {
        this.right = right;
        this.left = left;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitAssignment(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        right.print(printer);
        printer.print(" = ");
        left.print(printer);
    }
}
