package golite.ir.node;

import java.util.List;

import golite.ir.IrVisitor;
import golite.semantic.symbol.Function;
import golite.semantic.type.Type;
import golite.util.SourcePrinter;

/**
 *
 */
public class Call extends Expr<Type> implements Stmt {
    private final Function function;
    private final List<Expr<?>> arguments;

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public Call(Function function, List<Expr<?>> arguments) {
        super(function.getType().getReturnType().orElse(null));
        this.function = function;
        this.arguments = arguments;
    }

    public Function getFunction() {
        return function;
    }

    public List<Expr<?>> getArguments() {
        return arguments;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitCall(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("call(").print(function.getName());
        for (Expr<?> argument : arguments) {
            printer.print(", ");
            argument.print(printer);
        }
        printer.print(")");
    }
}
