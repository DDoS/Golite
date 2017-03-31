package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class JumpCond implements Stmt {
    private final Label label;
    private final Expr<BasicType> condition;

    public JumpCond(Label label, Expr<BasicType> condition) {
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
        visitor.visitJumpCond(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("jump(").print(label.getName()).print(", ");
        condition.print(printer);
        printer.print(")");
    }
}
