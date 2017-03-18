package golite.ir.node;

import golite.util.SourcePrinter;

/**
 *
 */
public class VoidReturn extends Stmt {
    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitVoidReturn(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("return");
    }
}
