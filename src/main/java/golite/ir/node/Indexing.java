package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.semantic.type.IndexableType;
import golite.semantic.type.Type;
import golite.util.SourcePrinter;

/**
 *
 */
public class Indexing<T extends IndexableType> extends Expr<Type> {
    private final Expr<T> value;
    private final Expr<BasicType> index;

    public Indexing(Expr<T> value, Expr<BasicType> index) {
        super(value.getType().getComponent());
        if (!index.getType().isInteger()) {
            throw new IllegalArgumentException("Expected an integer-typed index expression");
        }
        this.value = value;
        this.index = index;
    }

    public Expr<T> getValue() {
        return value;
    }

    public Expr<BasicType> getIndex() {
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
}
