package golite.ir.node;

import java.util.List;

import golite.ir.IrVisitor;
import golite.util.SourcePrinter;

/**
 *
 */
public class Program implements IrNode {
    private final String packageName;
    private final List<VariableDecl<?>> globals;
    private final List<FunctionDecl> functions;

    public Program(String packageName, List<VariableDecl<?>> globals, List<FunctionDecl> functions) {
        this.packageName = packageName;
        this.globals = globals;
        this.functions = functions;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<VariableDecl<?>> getGlobals() {
        return globals;
    }

    public List<FunctionDecl> getFunctions() {
        return functions;
    }

    @Override
    public void visit(IrVisitor visitor) {
        visitor.visitProgram(this);
    }

    @Override
    public void print(SourcePrinter printer) {
        printer.print("package ").print(packageName).newLine();
        functions.forEach(function -> {
            printer.newLine();
            function.print(printer);
            printer.newLine();
        });
    }
}
