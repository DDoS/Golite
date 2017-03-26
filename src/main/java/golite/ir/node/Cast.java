package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class Cast extends Expr<BasicType> implements Stmt {
    private final Expr<BasicType> argument;

    public Cast(BasicType type, Expr<BasicType> argument) {
        super(type);
        this.argument = argument;
    }

    public Expr<BasicType> getArgument() {
        return argument;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitCast(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("cast(").print(type.toString()).print(") ");
        argument.print(printer);
    }
}
