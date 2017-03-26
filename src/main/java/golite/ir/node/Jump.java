package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class Jump implements Stmt {
    private final Expr<BasicType> condition;
    private final Label label;

    public Jump(Expr<BasicType> condition, Label label) {
        if (condition.getType() != BasicType.BOOL) {
            throw new IllegalArgumentException("Expected a bool-typed expression");
        }
        this.condition = condition;
        this.label = label;
    }

    public Expr<BasicType> getCondition() {
        return condition;
    }

    public Label getLabel() {
        return label;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitJump(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("Jump(");
        label.print(printer);
        printer.print(", ");
        condition.print(printer);
        printer.print(")");
    }
}
