package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.IndexableType;
import golite.semantic.type.Type;
import golite.util.SourcePrinter;

/**
 *
 */
public class Indexing extends Expr {
    private final Expr value;
    private final Expr index;

    public Indexing(Expr value, Expr index) {
        super(checkIndexableType(value.getType()));
        if (!index.getType().isInteger()) {
            throw new IllegalArgumentException("Expected an integer-typed index expression");
        }
        this.value = value;
        this.index = index;
    }

    public Expr getValue() {
        return value;
    }

    public Expr getIndex() {
        return index;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitIndexing(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        value.print(printer);
        printer.print("[");
        index.print(printer);
        printer.print("]");
    }

    private static Type checkIndexableType(Type type) {
        if (!(type instanceof IndexableType)) {
            throw new IllegalArgumentException("Expected an indexable-typed value expression");
        }
        return ((IndexableType) type).getComponent();
    }
}
