package golite.ir.node;

import golite.semantic.symbol.Variable;
import golite.util.SourcePrinter;

/**
 *
 */
public class VariableDecl extends Stmt {
    private final Variable symbol;

    public VariableDecl(Variable symbol) {
        this.symbol = symbol;
    }

    public Variable getSymbol() {
        return symbol;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitVariableDecl(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("var ").print(symbol.getUniqueName()).print(" ").print(symbol.getType().resolve().toString());
    }
}
