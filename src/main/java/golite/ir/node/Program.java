package golite.ir.node;

import java.util.List;

import golite.util.SourcePrinter;

/**
 *
 */
public class Program extends IrNode {
    private final String packageName;
    private final List<FunctionDecl> functions;

    public Program(String packageName, List<FunctionDecl> functions) {
        this.packageName = packageName;
        this.functions = functions;
    }

    public String getPackageName() {
        return packageName;
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
