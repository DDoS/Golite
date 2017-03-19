package golite.ir.node;

import golite.semantic.symbol.Symbol;
import golite.util.SourcePrinter;

/**
 *
 */
public class Identifier extends Expr {
    private final Symbol symbol;

    public Identifier(Symbol symbol) {
        super(symbol.getType());
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitIdentifier(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print(symbol.getUniqueName());
    }
}
