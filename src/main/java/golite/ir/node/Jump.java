package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class Jump implements Stmt {
    private final Label label;
    private final Expr<BasicType> condition;

    public Jump(Label label, Expr<BasicType> condition) {
        if (condition.getType() != BasicType.BOOL) {
            throw new IllegalArgumentException("Expected a bool-typed expression");
        }
        this.condition = condition;
        this.label = label;
    }

    public Label getLabel() {
        return label;
    }

    public Expr<BasicType> getCondition() {
        return condition;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitJump(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("Jump(").print(label.getName()).print(", ");
        condition.print(printer);
        printer.print(")");
    }
}
