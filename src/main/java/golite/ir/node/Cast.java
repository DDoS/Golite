package golite.ir.node;

import golite.semantic.type.BasicType;
import golite.util.SourcePrinter;

/**
 *
 */
public class Cast extends Expr implements Stmt {
    private final Expr argument;

    public Cast(BasicType type, Expr argument) {
        super(type);
        this.argument = argument;
    }

    public Expr getArgument() {
        return argument;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitCast(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("cast(").print(type.toString()).print(", ");
        argument.print(printer);
        printer.print(")");
    }
}
