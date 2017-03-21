package golite.ir.node;

/**
 *
 */
public interface IrVisitor {
    void visitProgram(Program program);

    void visitFunctionDecl(FunctionDecl function);

    void visitVariableDecl(VariableDecl function);

    void visitVoidReturn(VoidReturn voidReturn);

    void visitValueReturn(ValueReturn valueReturn);

    void visitPrintBool(PrintBool printBool);

    void visitPrintInt(PrintInt printInt);

    void visitPrintRune(PrintRune printRune);

    void visitPrintFloat64(PrintFloat64 printFloat64);

    void visitPrintString(PrintString printString);

    void visitMemsetZero(MemsetZero memsetZero);

    void visitAssignment(Assignment assignment);

    void visitBoolLit(BoolLit boolLit);

    void visitIntLit(IntLit intLit);

    void visitFloatLit(Float64Lit float64Lit);

    void visitStringLit(StringLit stringLit);

    void visitIdentifier(Identifier identifier);

    void visitCall(Call call);

    void visitCast(Cast cast);
}
