package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class LogicNot extends Expr<BasicType> {
    private final Expr<BasicType> inner;

    public LogicNot(Expr<BasicType> inner) {
        super(BasicType.BOOL);
        if (inner.getType() != BasicType.BOOL) {
            throw new IllegalArgumentException("Expected a bool-typed expression");
        }
        this.inner = inner;
    }

    public Expr<BasicType> getInner() {
        return inner;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitLogicNot(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("!");
        inner.print(printer);
    }
}
