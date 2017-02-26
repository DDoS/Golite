package ca.sapon.golite;

import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import golite.analysis.AnalysisAdapter;
import golite.node.AAddExpr;
import golite.node.AAppendExpr;
import golite.node.AArrayType;
import golite.node.AAssignAddStmt;
import golite.node.AAssignBitAndNotStmt;
import golite.node.AAssignBitAndStmt;
import golite.node.AAssignBitOrStmt;
import golite.node.AAssignBitXorStmt;
import golite.node.AAssignDivStmt;
import golite.node.AAssignLshiftStmt;
import golite.node.AAssignMulStmt;
import golite.node.AAssignRemStmt;
import golite.node.AAssignRshiftStmt;
import golite.node.AAssignStmt;
import golite.node.AAssignSubStmt;
import golite.node.ABitAndExpr;
import golite.node.ABitAndNotExpr;
import golite.node.ABitNotExpr;
import golite.node.ABitOrExpr;
import golite.node.ABitXorExpr;
import golite.node.ABreakStmt;
import golite.node.ACallExpr;
import golite.node.ACastExpr;
import golite.node.AClauseForCondition;
import golite.node.AContinueStmt;
import golite.node.ADeclStmt;
import golite.node.ADeclVarShortStmt;
import golite.node.ADecrStmt;
import golite.node.ADefaultCase;
import golite.node.ADivExpr;
import golite.node.AEmptyForCondition;
import golite.node.AEmptyStmt;
import golite.node.AEqExpr;
import golite.node.AExprCase;
import golite.node.AExprForCondition;
import golite.node.AExprStmt;
import golite.node.AFloatExpr;
import golite.node.AForStmt;
import golite.node.AFuncDecl;
import golite.node.AGreatEqExpr;
import golite.node.AGreatExpr;
import golite.node.AIdentExpr;
import golite.node.AIfBlock;
import golite.node.AIfStmt;
import golite.node.AIncrStmt;
import golite.node.AIndexExpr;
import golite.node.AIntDecExpr;
import golite.node.AIntHexExpr;
import golite.node.AIntOctExpr;
import golite.node.ALessEqExpr;
import golite.node.ALessExpr;
import golite.node.ALogicAndExpr;
import golite.node.ALogicNotExpr;
import golite.node.ALogicOrExpr;
import golite.node.ALshiftExpr;
import golite.node.AMulExpr;
import golite.node.ANameType;
import golite.node.ANegateExpr;
import golite.node.ANeqExpr;
import golite.node.AParam;
import golite.node.APkg;
import golite.node.APrintStmt;
import golite.node.APrintlnStmt;
import golite.node.AProg;
import golite.node.AReaffirmExpr;
import golite.node.ARemExpr;
import golite.node.AReturnStmt;
import golite.node.ARshiftExpr;
import golite.node.ARuneExpr;
import golite.node.ASelectExpr;
import golite.node.ASliceType;
import golite.node.AStringIntrExpr;
import golite.node.AStringRawExpr;
import golite.node.AStructField;
import golite.node.AStructType;
import golite.node.ASubExpr;
import golite.node.ASwitchStmt;
import golite.node.ATypeDecl;
import golite.node.AVarDecl;
import golite.node.PDecl;
import golite.node.PExpr;
import golite.node.PIfBlock;
import golite.node.PParam;
import golite.node.PStmt;
import golite.node.PStructField;
import golite.node.Start;
import golite.node.TIdenf;

/**
 * The pretty printer.
 */
public class PrettyPrinter extends AnalysisAdapter {
    private final SourcePrinter printer;

    public PrettyPrinter(Writer output) {
        printer = new SourcePrinter(output);
    }

    @Override
    public void caseStart(Start node) {
        node.getPProg().apply(this);
    }

    @Override
    public void caseAProg(AProg node) {
        node.getPkg().apply(this);
        printer.newLine();
        for (PDecl decl : node.getDecl()) {
            printer.newLine();
            decl.apply(this);
            printer.newLine();
        }
    }

    @Override
    public void caseAPkg(APkg node) {
        printer.print("package ").print(node.getIdenf().getText());
    }

    @Override
    public void caseAVarDecl(AVarDecl node) {
        printer.print("var ");
        printIdenfList(node.getIdenf());
        if (node.getType() != null) {
            printer.print(" ");
            node.getType().apply(this);
        }
        if (!node.getExpr().isEmpty()) {
            printer.print(" = ");
            printExprList(node.getExpr());
        }
    }

    @Override
    public void caseATypeDecl(ATypeDecl node) {
        printer.print("type ").print(node.getIdenf().getText()).print(" ");
        node.getType().apply(this);
    }

    @Override
    public void caseAFuncDecl(AFuncDecl node) {
        printer.print("func ").print(node.getIdenf().getText()).print("(");
        final LinkedList<PParam> param = node.getParam();
        for (int i = 0, paramSize = param.size(); i < paramSize; i++) {
            param.get(i).apply(this);
            if (i < paramSize - 1) {
                printer.print(", ");
            }
        }
        printer.print(")");
        if (node.getType() != null) {
            printer.print(" ");
            node.getType().apply(this);
        }
        printer.print(" {").newLine().indent();
        printStmtList(node.getStmt());
        printer.dedent().print("}").newLine();
    }

    @Override
    public void caseAParam(AParam node) {
        printIdenfList(node.getIdenf());
        printer.print(" ");
        node.getType().apply(this);
    }

    @Override
    public void caseAIdentExpr(AIdentExpr node) {
        printer.print(node.getIdenf().getText());
    }

    @Override
    public void caseAIntDecExpr(AIntDecExpr node) {
        printer.print(node.getIntLit().getText());
    }

    @Override
    public void caseAIntOctExpr(AIntOctExpr node) {
        printer.print(node.getOctLit().getText());
    }

    @Override
    public void caseAIntHexExpr(AIntHexExpr node) {
        printer.print(node.getHexLit().getText());
    }

    @Override
    public void caseAFloatExpr(AFloatExpr node) {
        printer.print(node.getFloatLit().getText());
    }

    @Override
    public void caseARuneExpr(ARuneExpr node) {
        printer.print(node.getRuneLit().getText());
    }

    @Override
    public void caseAStringIntrExpr(AStringIntrExpr node) {
        printer.print(node.getInterpretedStringLit().getText());
    }

    @Override
    public void caseAStringRawExpr(AStringRawExpr node) {
        printer.print(node.getRawStringLit().getText());
    }

    @Override
    public void caseASelectExpr(ASelectExpr node) {
        printExprLeft(ASelectExpr.class, node.getValue());
        printer.print(".").print(node.getIdenf().getText());
    }

    @Override
    public void caseAIndexExpr(AIndexExpr node) {
        printExprLeft(AIndexExpr.class, node.getValue());
        printer.print("[");
        node.getIndex().apply(this);
        printer.print("]");
    }

    @Override
    public void caseACallExpr(ACallExpr node) {
        printExprLeft(ACallExpr.class, node.getValue());
        printer.print("(");
        printExprList(node.getArgs());
        printer.print(")");
    }

    @Override
    public void caseACastExpr(ACastExpr node) {
        node.getType().apply(this);
        printer.print("(");
        node.getValue().apply(this);
        printer.print(")");
    }

    @Override
    public void caseAAppendExpr(AAppendExpr node) {
        printer.print("append(");
        node.getLeft().apply(this);
        printer.print(", ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseALogicNotExpr(ALogicNotExpr node) {
        printer.print("!");
        printExprLeft(ALogicNotExpr.class, node.getInner());
    }

    @Override
    public void caseAReaffirmExpr(AReaffirmExpr node) {
        printer.print("+");
        // Need to add a space, otherwise it would be the ++ operator
        if (node.getInner().getClass() == AReaffirmExpr.class) {
            printer.print(" ");
        }
        printExprLeft(AReaffirmExpr.class, node.getInner());
    }

    @Override
    public void caseANegateExpr(ANegateExpr node) {
        printer.print("-");
        // Need to add a space, otherwise it would be the -- operator
        if (node.getInner().getClass() == ANegateExpr.class) {
            printer.print(" ");
        }
        printExprLeft(ANegateExpr.class, node.getInner());
    }

    @Override
    public void caseABitNotExpr(ABitNotExpr node) {
        printer.print("^");
        printExprLeft(ABitNotExpr.class, node.getInner());
    }

    @Override
    public void caseAMulExpr(AMulExpr node) {
        printExprLeft(AMulExpr.class, node.getLeft());
        printer.print(" * ");
        printExprRight(AMulExpr.class, node.getRight());
    }

    @Override
    public void caseADivExpr(ADivExpr node) {
        printExprLeft(ADivExpr.class, node.getLeft());
        printer.print(" / ");
        printExprRight(ADivExpr.class, node.getRight());
    }

    @Override
    public void caseARemExpr(ARemExpr node) {
        printExprLeft(ARemExpr.class, node.getLeft());
        printer.print(" % ");
        printExprRight(ARemExpr.class, node.getRight());
    }

    @Override
    public void caseALshiftExpr(ALshiftExpr node) {
        printExprLeft(ALshiftExpr.class, node.getLeft());
        printer.print(" << ");
        printExprRight(ALshiftExpr.class, node.getRight());
    }

    @Override
    public void caseARshiftExpr(ARshiftExpr node) {
        printExprLeft(ARshiftExpr.class, node.getLeft());
        printer.print(" >> ");
        printExprRight(ARshiftExpr.class, node.getRight());
    }

    @Override
    public void caseABitAndExpr(ABitAndExpr node) {
        printExprLeft(ABitAndExpr.class, node.getLeft());
        printer.print(" & ");
        printExprRight(ABitAndExpr.class, node.getRight());
    }

    @Override
    public void caseABitAndNotExpr(ABitAndNotExpr node) {
        printExprLeft(ABitAndNotExpr.class, node.getLeft());
        printer.print(" &^ ");
        printExprRight(ABitAndNotExpr.class, node.getRight());
    }

    @Override
    public void caseAAddExpr(AAddExpr node) {
        printExprLeft(AAddExpr.class, node.getLeft());
        printer.print(" + ");
        printExprRight(AAddExpr.class, node.getRight());
    }

    @Override
    public void caseASubExpr(ASubExpr node) {
        printExprLeft(ASubExpr.class, node.getLeft());
        printer.print(" - ");
        printExprRight(ASubExpr.class, node.getRight());
    }

    @Override
    public void caseABitOrExpr(ABitOrExpr node) {
        printExprLeft(ABitOrExpr.class, node.getLeft());
        printer.print(" | ");
        printExprRight(ABitOrExpr.class, node.getRight());
    }

    @Override
    public void caseABitXorExpr(ABitXorExpr node) {
        printExprLeft(ABitXorExpr.class, node.getLeft());
        printer.print(" ^ ");
        printExprRight(ABitXorExpr.class, node.getRight());
    }

    @Override
    public void caseAEqExpr(AEqExpr node) {
        printExprLeft(AEqExpr.class, node.getLeft());
        printer.print(" == ");
        printExprRight(AEqExpr.class, node.getRight());
    }

    @Override
    public void caseANeqExpr(ANeqExpr node) {
        printExprLeft(ANeqExpr.class, node.getLeft());
        printer.print(" != ");
        printExprRight(ANeqExpr.class, node.getRight());
    }

    @Override
    public void caseALessExpr(ALessExpr node) {
        printExprLeft(ALessExpr.class, node.getLeft());
        printer.print(" < ");
        printExprRight(ALessExpr.class, node.getRight());
    }

    @Override
    public void caseALessEqExpr(ALessEqExpr node) {
        printExprLeft(ALessEqExpr.class, node.getLeft());
        printer.print(" <= ");
        printExprRight(ALessEqExpr.class, node.getRight());
    }

    @Override
    public void caseAGreatExpr(AGreatExpr node) {
        printExprLeft(AGreatExpr.class, node.getLeft());
        printer.print(" > ");
        printExprRight(AGreatExpr.class, node.getRight());
    }

    @Override
    public void caseAGreatEqExpr(AGreatEqExpr node) {
        printExprLeft(AGreatEqExpr.class, node.getLeft());
        printer.print(" >= ");
        printExprRight(AGreatEqExpr.class, node.getRight());
    }

    @Override
    public void caseALogicAndExpr(ALogicAndExpr node) {
        printExprLeft(ALogicAndExpr.class, node.getLeft());
        printer.print(" && ");
        printExprRight(ALogicAndExpr.class, node.getRight());
    }

    @Override
    public void caseALogicOrExpr(ALogicOrExpr node) {
        printExprLeft(ALogicOrExpr.class, node.getLeft());
        printer.print(" || ");
        printExprRight(ALogicOrExpr.class, node.getRight());
    }

    @Override
    public void caseANameType(ANameType node) {
        printer.print(node.getIdenf().getText());
    }

    @Override
    public void caseASliceType(ASliceType node) {
        printer.print("[]");
        node.getType().apply(this);
    }

    @Override
    public void caseAArrayType(AArrayType node) {
        printer.print("[");
        node.getExpr().apply(this);
        printer.print("]");
        node.getType().apply(this);
    }

    @Override
    public void caseAStructType(AStructType node) {
        printer.print("struct {");
        final LinkedList<PStructField> fields = node.getFields();
        for (int i = 0, fieldsSize = fields.size(); i < fieldsSize; i++) {
            fields.get(i).apply(this);
            if (i < fieldsSize - 1) {
                printer.print(" ");
            }
        }
        printer.print("}");
    }

    @Override
    public void caseAStructField(AStructField node) {
        printIdenfList(node.getNames());
        printer.print(" ");
        node.getType().apply(this);
        printer.print(";");
    }

    // STATEMENTS
    @Override
    public void caseAEmptyStmt(AEmptyStmt node) {
    }

    @Override
    public void caseAPrintStmt(APrintStmt node) {
        printer.print("print(");
        printExprList(node.getExpr());
        printer.print(")");
    }

    @Override
    public void caseAPrintlnStmt(APrintlnStmt node) {
        printer.print("println(");
        printExprList(node.getExpr());
        printer.print(")");
    }

    @Override
    public void caseAContinueStmt(AContinueStmt node) {
        printer.print("continue");
    }

    @Override
    public void caseABreakStmt(ABreakStmt node) {
        printer.print("break");
    }

    @Override
    public void caseAReturnStmt(AReturnStmt node) {
        printer.print("return");
        if (node.getExpr() != null) {
            printer.print(" ");
            node.getExpr().apply(this);
        }
    }

    @Override
    public void caseAIfBlock(AIfBlock node) {
        printer.print("if");
        if (node.getInit() != null) {
            printer.print(" ");
            node.getInit().apply(this);
            printer.print(";");
        }
        printer.print(" ");
        node.getCond().apply(this);
        printer.print(" {").newLine().indent();
        printStmtList(node.getBlock());
        printer.dedent().print("}");
    }

    @Override
    public void caseAIfStmt(AIfStmt node) {
        final LinkedList<PIfBlock> ifBlocks = node.getIfBlock();
        for (int i = 0, ifBlocksSize = ifBlocks.size(); i < ifBlocksSize; i++) {
            ifBlocks.get(i).apply(this);
            if (i < ifBlocksSize - 1) {
                printer.print(" else ");
            }
        }
        if (!node.getElse().isEmpty()) {
            printer.print(" else {").newLine().indent();
            printStmtList(node.getElse());
            printer.dedent().print("}");
        }
    }

    @Override
    public void caseAAssignBitXorStmt(AAssignBitXorStmt node) {
        node.getLeft().apply(this);
        printer.print(" ^= ");
        node.getRight().apply(this);
    }

    @Override
    public void caseAAssignBitOrStmt(AAssignBitOrStmt node) {
        node.getLeft().apply(this);
        printer.print(" |= ");
        node.getRight().apply(this);
    }

    @Override
    public void caseAAssignSubStmt(AAssignSubStmt node) {
        node.getLeft().apply(this);
        printer.print(" -= ");
        node.getRight().apply(this);
    }

    @Override
    public void caseAAssignAddStmt(AAssignAddStmt node) {
        node.getLeft().apply(this);
        printer.print(" += ");
        node.getRight().apply(this);
    }

    @Override
    public void caseAAssignBitAndNotStmt(AAssignBitAndNotStmt node) {
        node.getLeft().apply(this);
        printer.print(" &^= ");
        node.getRight().apply(this);
    }

    @Override
    public void caseAAssignBitAndStmt(AAssignBitAndStmt node) {
        node.getLeft().apply(this);
        printer.print(" &= ");
        node.getRight().apply(this);
    }

    @Override
    public void caseAAssignRshiftStmt(AAssignRshiftStmt node) {
        node.getLeft().apply(this);
        printer.print(" >>= ");
        node.getRight().apply(this);
    }

    @Override
    public void caseAAssignLshiftStmt(AAssignLshiftStmt node) {
        node.getLeft().apply(this);
        printer.print(" <<= ");
        node.getRight().apply(this);
    }

    @Override
    public void caseAAssignRemStmt(AAssignRemStmt node) {
        node.getLeft().apply(this);
        printer.print(" %= ");
        node.getRight().apply(this);
    }

    @Override
    public void caseAAssignDivStmt(AAssignDivStmt node) {
        node.getLeft().apply(this);
        printer.print(" /= ");
        node.getRight().apply(this);
    }

    @Override
    public void caseAAssignMulStmt(AAssignMulStmt node) {
        node.getLeft().apply(this);
        printer.print(" *= ");
        node.getRight().apply(this);
    }

    @Override
    public void caseAAssignStmt(AAssignStmt node) {
        printExprList(node.getLeft());
        printer.print(" = ");
        printExprList(node.getRight());
    }

    @Override
    public void caseADeclVarShortStmt(ADeclVarShortStmt node) {
        printExprList(node.getLeft());
        printer.print(" := ");
        printExprList(node.getRight());
    }

    @Override
    public void caseADecrStmt(ADecrStmt node) {
        node.getExpr().apply(this);
        printer.print("--");
    }

    @Override
    public void caseAIncrStmt(AIncrStmt node) {
        node.getExpr().apply(this);
        printer.print("++");
    }

    @Override
    public void caseASwitchStmt(ASwitchStmt node) {
        printer.print("switch");
        if (node.getInit() != null) {
            printer.print(" ");
            node.getInit().apply(this);
            printer.print(";");
        }
        if (node.getValue() != null) {
            printer.print(" ");
            node.getValue().apply(this);
        }
        printer.print(" {").newLine();
        node.getCase().forEach(case_ -> case_.apply(this));
        printer.print("}");
    }

    @Override
    public void caseADefaultCase(ADefaultCase node) {
        printer.print("default:").newLine().indent();
        printStmtList(node.getStmt());
    }

    @Override
    public void caseAExprCase(AExprCase node) {
        printer.print("case ");
        printExprList(node.getExpr());
        printer.print(": ").newLine().indent();
        printStmtList(node.getStmt());
        printer.dedent();
    }

    @Override
    public void caseAEmptyForCondition(AEmptyForCondition node) {
    }

    @Override
    public void caseAExprForCondition(AExprForCondition node) {
        node.getExpr().apply(this);
    }

    @Override
    public void caseAClauseForCondition(AClauseForCondition node) {
        node.getInit().apply(this);
        printer.print("; ");
        node.getCond().apply(this);
        printer.print("; ");
        node.getPost().apply(this);
    }

    @Override
    public void caseAForStmt(AForStmt node) {
        printer.print("for ");
        node.getForCondition().apply(this);
        printer.print(" {").newLine().indent();
        // Statements
        printStmtList(node.getStmt());
        printer.dedent().print("}");
    }

    @Override
    public void caseADeclStmt(ADeclStmt node) {
        final LinkedList<PDecl> decls = node.getDecl();
        for (int i = 0, declsSize = decls.size(); i < declsSize; i++) {
            decls.get(i).apply(this);
            if (i < declsSize - 1) {
                printer.newLine();
            }
        }
    }

    @Override
    public void caseAExprStmt(AExprStmt node) {
        node.getExpr().apply(this);
    }

    private void printIdenfList(List<TIdenf> idenfs) {
        for (int i = 0, idenfsSize = idenfs.size(); i < idenfsSize; i++) {
            printer.print(idenfs.get(i).getText());
            if (i < idenfsSize - 1) {
                printer.print(", ");
            }
        }
    }

    private void printExprList(List<PExpr> exprs) {
        for (int i = 0, exprsSize = exprs.size(); i < exprsSize; i++) {
            exprs.get(i).apply(this);
            if (i < exprsSize - 1) {
                printer.print(", ");
            }
        }
    }

    private void printStmtList(List<PStmt> stmts) {
        for (PStmt stmt : stmts) {
            stmt.apply(this);
            printer.newLine();
        }
    }

    private void printExprLeft(Class<? extends PExpr> parent, PExpr child) {
        printExpr(parent, child, false);
    }

    private void printExprRight(Class<? extends PExpr> parent, PExpr child) {
        printExpr(parent, child, true);
    }

    private void printExpr(Class<? extends PExpr> parent, PExpr child, boolean right) {
        final Integer parentPrecedence = EXPR_PRECEDENCE.get(parent);
        if (parentPrecedence == null) {
            throw new IllegalStateException();
        }
        final Integer childPrecedence = EXPR_PRECEDENCE.get(child.getClass());
        if (childPrecedence == null) {
            throw new IllegalStateException();
        }
        // Add parenthesis around lower precedence children
        // If the children is on the right, also do so for the same precedence to respect left associativity
        final boolean needParenthesis = right ? childPrecedence >= parentPrecedence : childPrecedence > parentPrecedence;
        if (needParenthesis) {
            printer.print("(");
        }
        child.apply(this);
        if (needParenthesis) {
            printer.print(")");
        }
    }

    private static final Map<Class<? extends PExpr>, Integer> EXPR_PRECEDENCE;

    static {
        final Map<Class<? extends PExpr>, Integer> precedences = new HashMap<>();
        // Precedence -2
        precedences.putAll(Stream.of(
                AIdentExpr.class, AIntDecExpr.class, AIntOctExpr.class, AIntHexExpr.class, AFloatExpr.class, ARuneExpr.class,
                AStringIntrExpr.class, AStringRawExpr.class, AAppendExpr.class
        ).collect(Collectors.toMap(key -> key, value -> -1)));
        // Precedence -1
        precedences.putAll(Stream.of(
                ASelectExpr.class, AIndexExpr.class, ACallExpr.class, ACastExpr.class
        ).collect(Collectors.toMap(key -> key, value -> -1)));
        // Precedence 0
        precedences.putAll(Stream.of(
                ALogicNotExpr.class, AReaffirmExpr.class, ANegateExpr.class, ABitNotExpr.class
        ).collect(Collectors.toMap(key -> key, value -> 0)));
        // Precedence 1
        precedences.putAll(Stream.of(
                AMulExpr.class, ADivExpr.class, ARemExpr.class, ALshiftExpr.class, ARshiftExpr.class, ABitAndExpr.class, ABitAndNotExpr.class
        ).collect(Collectors.toMap(key -> key, value -> 1)));
        // Precedence 2
        precedences.putAll(Stream.of(
                AAddExpr.class, ASubExpr.class, ABitOrExpr.class, ABitXorExpr.class
        ).collect(Collectors.toMap(key -> key, value -> 2)));
        // Precedence 3
        precedences.putAll(Stream.of(
                AEqExpr.class, ANeqExpr.class, ALessExpr.class, ALessEqExpr.class, AGreatExpr.class, AGreatEqExpr.class
        ).collect(Collectors.toMap(key -> key, value -> 3)));
        // Precedence 4
        precedences.putAll(Stream.of(
                ALogicAndExpr.class
        ).collect(Collectors.toMap(key -> key, value -> 4)));
        // Precedence 5
        precedences.putAll(Stream.of(
                ALogicOrExpr.class
        ).collect(Collectors.toMap(key -> key, value -> 5)));
        EXPR_PRECEDENCE = Collections.unmodifiableMap(precedences);
    }
  
}
