package golite.ir;

/**
 *
 */
public interface IrVisitor {
    void visitProgram(Program program);

    void visitFunctionDecl(FunctionDecl function);

    void visitPrintInt(PrintInt printInt);

    void visitIntLit(IntLit intLit);
}
