package golite.ir.node;

import golite.ir.IrVisitor;
import golite.util.SourcePrinter;

/**
 *
 */
public class Label implements Stmt {
    private final String name;

    public Label(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitLabel(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.dedent().print(name).print(":").indent();
    }
}
