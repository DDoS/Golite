package ca.sapon.golite;

import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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
import golite.node.PExpr;
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
        node.getPExpr().apply(this);
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
        final LinkedList<PExpr> args = node.getArgs();
        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            args.get(i).apply(this);
            if (i < argsSize - 1) {
                printer.print(", ");
            }
        }
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
        printExpr(printer, AMulExpr.class, node.getLeft());
        printer.print(" * ");
        printExpr(printer, AMulExpr.class, node.getRight());
    }

    @Override
    public void caseADivExpr(ADivExpr node) {
        printExpr(printer, ADivExpr.class, node.getLeft());
        printer.print(" / ");
        printExpr(printer, ADivExpr.class, node.getRight());
    }

    @Override
    public void caseARemExpr(ARemExpr node) {
        printExpr(printer, ARemExpr.class, node.getLeft());
        printer.print(" % ");
        printExpr(printer, ARemExpr.class, node.getRight());
    }

    @Override
    public void caseALshiftExpr(ALshiftExpr node) {
        printExpr(printer, ALshiftExpr.class, node.getLeft());
        printer.print(" << ");
        printExpr(printer, ALshiftExpr.class, node.getRight());
    }

    @Override
    public void caseARshiftExpr(ARshiftExpr node) {
        printExpr(printer, ARshiftExpr.class, node.getLeft());
        printer.print(" >> ");
        printExpr(printer, ARshiftExpr.class, node.getRight());
    }

    @Override
    public void caseABitAndExpr(ABitAndExpr node) {
        printExpr(printer, ABitAndExpr.class, node.getLeft());
        printer.print(" & ");
        printExpr(printer, ABitAndExpr.class, node.getRight());
    }

    @Override
    public void caseABitAndNotExpr(ABitAndNotExpr node) {
        printExpr(printer, ABitAndNotExpr.class, node.getLeft());
        printer.print(" &^ ");
        printExpr(printer, ABitAndNotExpr.class, node.getRight());
    }

    @Override
    public void caseAAddExpr(AAddExpr node) {
        printExpr(printer, AAddExpr.class, node.getLeft());
        printer.print(" + ");
        printExpr(printer, AAddExpr.class, node.getRight());
    }

    @Override
    public void caseASubExpr(ASubExpr node) {
        printExpr(printer, ASubExpr.class, node.getLeft());
        printer.print(" - ");
        printExpr(printer, ASubExpr.class, node.getRight());
    }

    @Override
    public void caseABitOrExpr(ABitOrExpr node) {
        printExpr(printer, ABitOrExpr.class, node.getLeft());
        printer.print(" | ");
        printExpr(printer, ABitOrExpr.class, node.getRight());
    }

    @Override
    public void caseABitXorExpr(ABitXorExpr node) {
        printExpr(printer, ABitXorExpr.class, node.getLeft());
        printer.print(" ^ ");
        printExpr(printer, ABitXorExpr.class, node.getRight());
    }

    @Override
    public void caseAEqExpr(AEqExpr node) {
        printExpr(printer, AEqExpr.class, node.getLeft());
        printer.print(" == ");
        printExpr(printer, AEqExpr.class, node.getRight());
    }

    @Override
    public void caseANeqExpr(ANeqExpr node) {
        printExpr(printer, ANeqExpr.class, node.getLeft());
        printer.print(" != ");
        printExpr(printer, ANeqExpr.class, node.getRight());
    }

    @Override
    public void caseALessExpr(ALessExpr node) {
        printExpr(printer, ALessExpr.class, node.getLeft());
        printer.print(" < ");
        printExpr(printer, ALessExpr.class, node.getRight());
    }

    @Override
    public void caseALessEqExpr(ALessEqExpr node) {
        printExpr(printer, ALessEqExpr.class, node.getLeft());
        printer.print(" <= ");
        printExpr(printer, ALessEqExpr.class, node.getRight());
    }

    @Override
    public void caseAGreatExpr(AGreatExpr node) {
        printExpr(printer, AGreatExpr.class, node.getLeft());
        printer.print(" > ");
        printExpr(printer, AGreatExpr.class, node.getRight());
    }

    @Override
    public void caseAGreatEqExpr(AGreatEqExpr node) {
        printExpr(printer, AGreatEqExpr.class, node.getLeft());
        printer.print(" >= ");
        printExpr(printer, AGreatEqExpr.class, node.getRight());
    }

    @Override
    public void caseALogicAndExpr(ALogicAndExpr node) {
        printExpr(printer, ALogicAndExpr.class, node.getLeft());
        printer.print(" && ");
        printExpr(printer, ALogicAndExpr.class, node.getRight());
    }

    @Override
    public void caseALogicOrExpr(ALogicOrExpr node) {
        printExpr(printer, ALogicOrExpr.class, node.getLeft());
        printer.print(" || ");
        printExpr(printer, ALogicOrExpr.class, node.getRight());
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
        printer.print("struct {").newLine().indent();
        node.getFields().forEach(field -> {
            field.apply(this);
            printer.newLine();
        });
        printer.dedent().print("}");
    }

    @Override
    public void caseAStructField(AStructField node) {
        final LinkedList<TIdenf> names = node.getNames();
        for (int i = 0, namesSize = names.size(); i < namesSize; i++) {
            printer.print(names.get(i).getText());
            if (i < namesSize - 1) {
                printer.print(", ");
            }
        }
        printer.print(" ");
        node.getType().apply(this);
        printer.print(";");
    }

    public void printExpr(SourcePrinter printer, Class<? extends PExpr> parent, PExpr child) {
        final boolean needParenthesis = lowerPrecedence(child.getClass(), parent);
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

    public static boolean lowerPrecedence(Class<? extends PExpr> from, Class<? extends PExpr> to) {
        final int fromPrecedence = EXPR_PRECEDENCE.getOrDefault(from, 0);
        final int toPrecedence = EXPR_PRECEDENCE.getOrDefault(to, 0);
        if (toPrecedence <= 0) {
            throw new IllegalStateException();
        }
        return fromPrecedence > toPrecedence;
    }
}
