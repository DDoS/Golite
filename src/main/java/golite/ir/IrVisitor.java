package golite.ir;

import golite.ir.node.Append;
import golite.ir.node.Assignment;
import golite.ir.node.BinArFloat64;
import golite.ir.node.BinArInt;
import golite.ir.node.BoolLit;
import golite.ir.node.Call;
import golite.ir.node.Cast;
import golite.ir.node.CmpArray;
import golite.ir.node.CmpBool;
import golite.ir.node.CmpFloat64;
import golite.ir.node.CmpInt;
import golite.ir.node.CmpString;
import golite.ir.node.CmpStruct;
import golite.ir.node.ConcatString;
import golite.ir.node.Float64Lit;
import golite.ir.node.FunctionDecl;
import golite.ir.node.Identifier;
import golite.ir.node.Indexing;
import golite.ir.node.IntLit;
import golite.ir.node.Jump;
import golite.ir.node.Label;
import golite.ir.node.LogicAnd;
import golite.ir.node.LogicNot;
import golite.ir.node.LogicOr;
import golite.ir.node.MemsetZero;
import golite.ir.node.PrintBool;
import golite.ir.node.PrintFloat64;
import golite.ir.node.PrintInt;
import golite.ir.node.PrintRune;
import golite.ir.node.PrintString;
import golite.ir.node.Program;
import golite.ir.node.Select;
import golite.ir.node.StringLit;
import golite.ir.node.UnaArFloat64;
import golite.ir.node.UnaArInt;
import golite.ir.node.ValueReturn;
import golite.ir.node.VariableDecl;
import golite.ir.node.VoidReturn;

/**
 *
 */
public interface IrVisitor {
    void visitProgram(Program program);

    void visitFunctionDecl(FunctionDecl function);

    void visitVariableDecl(VariableDecl<?> variableDecl);

    void visitVoidReturn(VoidReturn voidReturn);

    void visitValueReturn(ValueReturn valueReturn);

    void visitPrintBool(PrintBool printBool);

    void visitPrintInt(PrintInt printInt);

    void visitPrintRune(PrintRune printRune);

    void visitPrintFloat64(PrintFloat64 printFloat64);

    void visitPrintString(PrintString printString);

    void visitMemsetZero(MemsetZero memsetZero);

    void visitAssignment(Assignment assignment);

    void visitJump(Jump jump);

    void visitLabel(Label label);

    void visitBoolLit(BoolLit boolLit);

    void visitIntLit(IntLit intLit);

    void visitFloatLit(Float64Lit float64Lit);

    void visitStringLit(StringLit stringLit);

    void visitIdentifier(Identifier<?> identifier);

    void visitSelect(Select select);

    void visitIndexing(Indexing indexing);

    void visitCall(Call call);

    void visitCast(Cast cast);

    void visitAppend(Append append);

    void visitLogicNot(LogicNot logicNot);

    void visitUnaArInt(UnaArInt unaArInt);

    void visitUnaArFloat64(UnaArFloat64 unaArFloat64);

    void visitBinArInt(BinArInt binArInt);

    void visitConcatString(ConcatString concatString);

    void visitBinArFloat64(BinArFloat64 binArFloat64);

    void visitCmpBool(CmpBool cmpBool);

    void visitCmpInt(CmpInt cmpInt);

    void visitCmpFloat64(CmpFloat64 cmpFloat64);

    void visitCmpString(CmpString cmpString);

    void visitCmpArray(CmpArray cmpArray);

    void visitCmpStruct(CmpStruct cmpStruct);

    void visitLogicAnd(LogicAnd logicAnd);

    void visitLogicOr(LogicOr logicOr);
}
