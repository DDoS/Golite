package golite.ir.node;

import golite.util.SourcePrinter;

/**
 *
 */
public interface IrNode {
    void visit(IrVisitor visitor);

    void print(SourcePrinter printer);
}
