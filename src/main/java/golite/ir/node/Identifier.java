package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.symbol.Variable;
import golite.util.SourcePrinter;

/**
 *
 */
public class Identifier extends Expr {
    private final Variable variable;
    private final String uniqueName;

    public Identifier(Variable variable, String uniqueName) {
        super(variable.getType());
        this.variable = variable;
        this.uniqueName = uniqueName;
    }

    public Variable getVariable() {
        return variable;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitIdentifier(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print(uniqueName);
    }

    @Override
    public String toString() {
        return uniqueName;
    }
}
