package golite.ir;

import golite.util.SourcePrinter;

/**
 *
 */
public abstract class IrNode {
    public abstract void visit(IrVisitor visitor);

    public abstract void print(SourcePrinter printer);
}
