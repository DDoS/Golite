package golite.ir.node;

/**
 *
 */
public interface IrVisitor {
    void visitProgram(Program program);

    void visitFunctionDecl(FunctionDecl function);

    void visitVariableDecl(VariableDecl function);

    void visitVoidReturn(VoidReturn voidReturn);

    void visitPrintBool(PrintBool printBool);

    void visitPrintInt(PrintInt printInt);

    void visitPrintString(PrintString printString);

    void visitAssignment(Assignment assignment);

    void visitBoolLit(BoolLit boolLit);

    void visitIntLit(IntLit intLit);

    void visitFloatLit(FloatLit floatLit);

    void visitStringLit(StringLit stringLit);

    void visitIdentifier(Identifier identifier);
}
