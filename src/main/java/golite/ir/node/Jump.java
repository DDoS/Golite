package golite.ir.node;

import golite.ir.IrVisitor;
import golite.util.SourcePrinter;

/**
 *
 */
public class Jump implements Stmt {
    private final Label label;

    public Jump(Label label) {
        this.label = label;
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
        printer.print("jump(").print(label.getName()).print(")");
    }
}
