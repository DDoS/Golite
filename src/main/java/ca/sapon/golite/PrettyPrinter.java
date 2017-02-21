package ca.sapon.golite;

import java.io.OutputStream;
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
import golite.node.ABitAndExpr;
import golite.node.ABitAndNotExpr;
import golite.node.ABitNotExpr;
import golite.node.ABitOrExpr;
import golite.node.ABitXorExpr;
import golite.node.ACallExpr;
import golite.node.ACastExpr;
import golite.node.ADivExpr;
import golite.node.AEqExpr;
import golite.node.AFloatExpr;
import golite.node.AFuncDecl;
import golite.node.AGreatEqExpr;
import golite.node.AGreatExpr;
import golite.node.AIdentExpr;
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
import golite.node.AProg;
import golite.node.AReaffirmExpr;
import golite.node.ARemExpr;
import golite.node.ARshiftExpr;
import golite.node.ARuneExpr;
import golite.node.ASelectExpr;
import golite.node.ASliceType;
import golite.node.AStringIntrExpr;
import golite.node.AStringRawExpr;
import golite.node.AStructField;
import golite.node.AStructType;
import golite.node.ASubExpr;
import golite.node.ATypeDecl;
import golite.node.AVarDecl;
import golite.node.PDecl;
import golite.node.PExpr;
import golite.node.PParam;
import golite.node.PStmtStub;
import golite.node.PStructField;
import golite.node.Start;
import golite.node.TIdenf;

/**
 * The pretty printer.
 */
public class PrettyPrinter extends AnalysisAdapter {
    private final SourcePrinter printer;

    public PrettyPrinter(OutputStream output) {
        printer = new SourcePrinter(output);
    }

    @Override
    public void caseStart(Start node) {
        node.getPProg().apply(this);
    }

    @Override
    public void caseAProg(AProg node) {
        node.getPkg().apply(this);
        for (PDecl decl : node.getDecl()) {
            printer.newLine();
            decl.apply(this);
        }
    }

    @Override
    public void caseAPkg(APkg node) {
        printer.print("package ").print(node.getIdenf().getText()).print(";").newLine();
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
        printer.print(";").newLine();
    }

    @Override
    public void caseATypeDecl(ATypeDecl node) {
        printer.print("type ").print(node.getIdenf().getText()).print(" ");
        node.getType().apply(this);
        printer.print(";").newLine();
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
        for (PStmtStub stmt : node.getStmtStub()) {
            stmt.apply(this);
        }
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
        node.getValue().apply(this);
        printer.print(".").print(node.getIdenf().getText());
    }

    @Override
    public void caseAIndexExpr(AIndexExpr node) {
        node.getValue().apply(this);
        printer.print("[");
        node.getIndex().apply(this);
        printer.print("]");
    }

    @Override
    public void caseACallExpr(ACallExpr node) {
        node.getValue().apply(this);
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
        node.getInner().apply(this);
    }

    @Override
    public void caseAReaffirmExpr(AReaffirmExpr node) {
        printer.print("+");
        node.getInner().apply(this);
    }

    @Override
    public void caseANegateExpr(ANegateExpr node) {
        printer.print("-");
        node.getInner().apply(this);
    }

    @Override
    public void caseABitNotExpr(ABitNotExpr node) {
        printer.print("^");
        node.getInner().apply(this);
    }

    @Override
    public void caseAMulExpr(AMulExpr node) {
        printExprLeft(printer, AMulExpr.class, node.getLeft());
        printer.print(" * ");
        printExprRight(printer, AMulExpr.class, node.getRight());
    }

    @Override
    public void caseADivExpr(ADivExpr node) {
        printExprLeft(printer, ADivExpr.class, node.getLeft());
        printer.print(" / ");
        printExprRight(printer, ADivExpr.class, node.getRight());
    }

    @Override
    public void caseARemExpr(ARemExpr node) {
        printExprLeft(printer, ARemExpr.class, node.getLeft());
        printer.print(" % ");
        printExprRight(printer, ARemExpr.class, node.getRight());
    }

    @Override
    public void caseALshiftExpr(ALshiftExpr node) {
        printExprLeft(printer, ALshiftExpr.class, node.getLeft());
        printer.print(" << ");
        printExprRight(printer, ALshiftExpr.class, node.getRight());
    }

    @Override
    public void caseARshiftExpr(ARshiftExpr node) {
        printExprLeft(printer, ARshiftExpr.class, node.getLeft());
        printer.print(" >> ");
        printExprRight(printer, ARshiftExpr.class, node.getRight());
    }

    @Override
    public void caseABitAndExpr(ABitAndExpr node) {
        printExprLeft(printer, ABitAndExpr.class, node.getLeft());
        printer.print(" & ");
        printExprRight(printer, ABitAndExpr.class, node.getRight());
    }

    @Override
    public void caseABitAndNotExpr(ABitAndNotExpr node) {
        printExprLeft(printer, ABitAndNotExpr.class, node.getLeft());
        printer.print(" &^ ");
        printExprRight(printer, ABitAndNotExpr.class, node.getRight());
    }

    @Override
    public void caseAAddExpr(AAddExpr node) {
        printExprLeft(printer, AAddExpr.class, node.getLeft());
        printer.print(" + ");
        printExprRight(printer, AAddExpr.class, node.getRight());
    }

    @Override
    public void caseASubExpr(ASubExpr node) {
        printExprLeft(printer, ASubExpr.class, node.getLeft());
        printer.print(" - ");
        printExprRight(printer, ASubExpr.class, node.getRight());
    }

    @Override
    public void caseABitOrExpr(ABitOrExpr node) {
        printExprLeft(printer, ABitOrExpr.class, node.getLeft());
        printer.print(" | ");
        printExprRight(printer, ABitOrExpr.class, node.getRight());
    }

    @Override
    public void caseABitXorExpr(ABitXorExpr node) {
        printExprLeft(printer, ABitXorExpr.class, node.getLeft());
        printer.print(" ^ ");
        printExprRight(printer, ABitXorExpr.class, node.getRight());
    }

    @Override
    public void caseAEqExpr(AEqExpr node) {
        printExprLeft(printer, AEqExpr.class, node.getLeft());
        printer.print(" == ");
        printExprRight(printer, AEqExpr.class, node.getRight());
    }

    @Override
    public void caseANeqExpr(ANeqExpr node) {
        printExprLeft(printer, ANeqExpr.class, node.getLeft());
        printer.print(" != ");
        printExprRight(printer, ANeqExpr.class, node.getRight());
    }

    @Override
    public void caseALessExpr(ALessExpr node) {
        printExprLeft(printer, ALessExpr.class, node.getLeft());
        printer.print(" < ");
        printExprRight(printer, ALessExpr.class, node.getRight());
    }

    @Override
    public void caseALessEqExpr(ALessEqExpr node) {
        printExprLeft(printer, ALessEqExpr.class, node.getLeft());
        printer.print(" <= ");
        printExprRight(printer, ALessEqExpr.class, node.getRight());
    }

    @Override
    public void caseAGreatExpr(AGreatExpr node) {
        printExprLeft(printer, AGreatExpr.class, node.getLeft());
        printer.print(" > ");
        printExprRight(printer, AGreatExpr.class, node.getRight());
    }

    @Override
    public void caseAGreatEqExpr(AGreatEqExpr node) {
        printExprLeft(printer, AGreatEqExpr.class, node.getLeft());
        printer.print(" >= ");
        printExprRight(printer, AGreatEqExpr.class, node.getRight());
    }

    @Override
    public void caseALogicAndExpr(ALogicAndExpr node) {
        printExprLeft(printer, ALogicAndExpr.class, node.getLeft());
        printer.print(" && ");
        printExprRight(printer, ALogicAndExpr.class, node.getRight());
    }

    @Override
    public void caseALogicOrExpr(ALogicOrExpr node) {
        printExprLeft(printer, ALogicOrExpr.class, node.getLeft());
        printer.print(" || ");
        printExprRight(printer, ALogicOrExpr.class, node.getRight());
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

    private void printExprLeft(SourcePrinter printer, Class<? extends PExpr> parent, PExpr child) {
        printExpr(printer, parent, child, false);
    }

    private void printExprRight(SourcePrinter printer, Class<? extends PExpr> parent, PExpr child) {
        printExpr(printer, parent, child, true);
    }

    private void printExpr(SourcePrinter printer, Class<? extends PExpr> parent, PExpr child, boolean right) {
        final int parentPrecedence = EXPR_PRECEDENCE.getOrDefault(parent, 0);
        if (parentPrecedence <= 0) {
            throw new IllegalStateException();
        }
        final int childPrecedence = EXPR_PRECEDENCE.getOrDefault(child.getClass(), 0);
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
