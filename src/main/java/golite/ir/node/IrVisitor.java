package golite.ir.node;

/**
 *
 */
public interface IrVisitor {
    void visitProgram(Program program);

    void visitFunctionDecl(FunctionDecl function);

    void visitVoidReturn(VoidReturn voidReturn);

    void visitPrintInt(PrintInt printInt);

    void visitIntLit(IntLit intLit);
}
