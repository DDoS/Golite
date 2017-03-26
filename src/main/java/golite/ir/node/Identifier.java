package golite.ir.node;

import golite.ir.IrVisitor;
import golite.semantic.symbol.Variable;
import golite.semantic.type.Type;
import golite.util.SourcePrinter;

/**
 *
 */
public class Identifier<T extends Type> extends Expr<T> {
    private final Variable<T> variable;
    private final String uniqueName;

    public Identifier(Variable<T> variable) {
        this(variable, variable.getName());
    }

    public Identifier(Variable<T> variable, String uniqueName) {
        super(variable.getType());
        this.variable = variable;
        this.uniqueName = uniqueName;
    }

    public Variable<T> getVariable() {
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
