package golite.ir.node;

import golite.semantic.symbol.Variable;
import golite.util.SourcePrinter;

/**
 *
 */
public class VariableDecl extends Stmt {
    private final Variable symbol;
    private final String uniqueName;

    public VariableDecl(Variable symbol, String uniqueName) {
        this.symbol = symbol;
        this.uniqueName = uniqueName;
    }

    public Variable getSymbol() {
        return symbol;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitVariableDecl(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("var ").print(uniqueName).print(" ").print(symbol.getType().toString());
    }
}
