package golite.ir.node;

import java.util.List;

import golite.semantic.symbol.Function;
import golite.semantic.symbol.Variable;
import golite.util.SourcePrinter;

/**
 *
 */
public class FunctionDecl implements IrNode {
    private final Function function;
    private final List<String> paramUniqueNames;
    private final List<Stmt> statements;

    public FunctionDecl(Function function, List<String> paramUniqueNames, List<Stmt> statements) {
        this.function = function;
        this.paramUniqueNames = paramUniqueNames;
        this.statements = statements;
    }

    public Function getFunction() {
        return function;
    }

    public List<String> getParamUniqueNames() {
        return paramUniqueNames;
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
        printer.print("func ").print(function.getName()).print("(");
        final List<Variable> parameters = function.getParameters();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            printer.print(paramUniqueNames.get(i)).print(" ").print(parameters.get(i).getType().toString());
            if (i < parametersSize - 1) {
                printer.print(", ");
            }
        }
        printer.print(") {").newLine().indent();
        statements.forEach(stmt -> {
            stmt.print(printer);
            printer.newLine();
        });
        printer.dedent().print("}");
    }
}