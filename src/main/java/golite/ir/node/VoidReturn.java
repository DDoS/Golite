package golite.ir.node;

import golite.ir.IrVisitor;
import golite.util.SourcePrinter;

/**
 *
 */
public class VoidReturn implements Stmt {
    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitVoidReturn(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("return");
    }
}
