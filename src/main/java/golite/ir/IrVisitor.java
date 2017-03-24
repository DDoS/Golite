package golite.ir;

import golite.ir.node.Assignment;
import golite.ir.node.BoolLit;
import golite.ir.node.Call;
import golite.ir.node.Cast;
import golite.ir.node.Float64Lit;
import golite.ir.node.FunctionDecl;
import golite.ir.node.Identifier;
import golite.ir.node.Indexing;
import golite.ir.node.IntLit;
import golite.ir.node.MemsetZero;
import golite.ir.node.PrintBool;
import golite.ir.node.PrintFloat64;
import golite.ir.node.PrintInt;
import golite.ir.node.PrintRune;
import golite.ir.node.PrintString;
import golite.ir.node.Program;
import golite.ir.node.Select;
import golite.ir.node.StringLit;
import golite.ir.node.ValueReturn;
import golite.ir.node.VariableDecl;
import golite.ir.node.VoidReturn;

/**
 *
 */
public interface IrVisitor {
    void visitProgram(Program program);

    void visitFunctionDecl(FunctionDecl function);

    void visitVariableDecl(VariableDecl variableDecl);

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

    void visitSelect(Select select);

    void visitIndexing(Indexing indexing);

    void visitCall(Call call);

    void visitCast(Cast cast);
}
