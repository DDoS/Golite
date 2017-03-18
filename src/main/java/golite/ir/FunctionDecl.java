package golite.ir;

import java.util.List;

import golite.semantic.symbol.Function;
import golite.util.SourcePrinter;

/**
 *
 */
public class FunctionDecl extends IrNode {
    private final Function symbol;
    private final List<Stmt> statements;

    public FunctionDecl(Function symbol, List<Stmt> statements) {
        this.symbol = symbol;
        this.statements = statements;
    }

    public Function getSymbol() {
        return symbol;
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
        printer.print(symbol.toString()).print(" {").newLine().indent();
        statements.forEach(stmt -> {
            stmt.print(printer);
            printer.newLine();
        });
        printer.dedent().print("}");
    }
}
