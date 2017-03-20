package golite.ir.node;

import java.util.List;

import golite.semantic.symbol.Function;
import golite.util.SourcePrinter;

/**
 *
 */
public class FunctionDecl implements IrNode {
    private final Function function;
    private final List<Stmt> statements;

    public FunctionDecl(Function function, List<Stmt> statements) {
        this.function = function;
        this.statements = statements;
    }

    public Function getFunction() {
        return function;
    }

    public List<Stmt> getStatements() {
        return statements;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitFunctionDecl(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print(function.toString()).print(" {").newLine().indent();
        statements.forEach(stmt -> {
            stmt.print(printer);
            printer.newLine();
        });
        printer.dedent().print("}");
    }
}
