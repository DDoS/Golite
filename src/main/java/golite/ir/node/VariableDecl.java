package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.symbol.Variable;
import golite.semantic.type.Type;
import golite.util.SourcePrinter;

/**
 *
 */
public class VariableDecl<T extends Type> implements Stmt {
    private final Variable<T> symbol;
    private final String uniqueName;

    public VariableDecl(Variable<T> variable) {
        this(variable, variable.getName());
    }

    public VariableDecl(Variable<T> symbol, String uniqueName) {
        this.symbol = symbol;
        this.uniqueName = uniqueName;
    }

    public Variable<T> getVariable() {
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
