package golite.ir.node;

import java.io.StringWriter;

import golite.ir.IrVisitor;
import golite.util.SourcePrinter;

/**
 *
 */
public interface IrNode {
    void visit(IrVisitor visitor);

    void print(SourcePrinter printer);

    static String toString(IrNode node) {
        final StringWriter writer = new StringWriter();
        node.print(new SourcePrinter(writer));
        return writer.toString();
    }
}
