package ca.sapon.golite;

import java.io.OutputStream;
import java.util.LinkedList;

import golite.analysis.AnalysisAdapter;
import golite.node.AAddExpr;
import golite.node.AAppendExpr;
import golite.node.AArrayType;
import golite.node.ABitAndExpr;
import golite.node.ABitAndNotExpr;
import golite.node.ABitNotExpr;
import golite.node.ABitOrExpr;
import golite.node.ABitXorExpr;
import golite.node.ABoolType;
import golite.node.ACallExpr;
import golite.node.ACastExpr;
import golite.node.ADivExpr;
import golite.node.AEqExpr;
import golite.node.AFloat64Type;
import golite.node.AFloatExpr;
import golite.node.AGreatEqExpr;
import golite.node.AGreatExpr;
import golite.node.AIdentExpr;
import golite.node.AIndexExpr;
import golite.node.AIntDecExpr;
import golite.node.AIntHexExpr;
import golite.node.AIntOctExpr;
import golite.node.AIntType;
import golite.node.ALessEqExpr;
import golite.node.ALessExpr;
import golite.node.ALogicAndExpr;
import golite.node.ALogicNotExpr;
import golite.node.ALogicOrExpr;
import golite.node.ALshiftExpr;
import golite.node.AMulExpr;
import golite.node.ANegateExpr;
import golite.node.ANeqExpr;
import golite.node.AReaffirmExpr;
import golite.node.ARemExpr;
import golite.node.ARshiftExpr;
import golite.node.ARuneExpr;
import golite.node.ARuneType;
import golite.node.ASelectExpr;
import golite.node.ASliceType;
import golite.node.AStringIntrExpr;
import golite.node.AStringRawExpr;
import golite.node.AStringType;
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
        printer.print(node.getIntDtype().getText());
    }

    @Override
    public void caseAIntOctExpr(AIntOctExpr node) {
        printer.print(node.getOctDtype().getText());
    }

    @Override
    public void caseAIntHexExpr(AIntHexExpr node) {
        printer.print(node.getHexDtype().getText());
    }

    @Override
    public void caseAFloatExpr(AFloatExpr node) {
        printer.print(node.getFloatDtype().getText());
    }

    @Override
    public void caseARuneExpr(ARuneExpr node) {
        printer.print(node.getRuneDtype().getText());
    }

    @Override
    public void caseAStringIntrExpr(AStringIntrExpr node) {
        printer.print(node.getInterpretedStringDtype().getText());
    }

    @Override
    public void caseAStringRawExpr(AStringRawExpr node) {
        printer.print(node.getRawStringDtype().getText());
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
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" * ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseADivExpr(ADivExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" / ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseARemExpr(ARemExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" % ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseALshiftExpr(ALshiftExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" << ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseARshiftExpr(ARshiftExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" >> ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseABitAndExpr(ABitAndExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" & ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseABitAndNotExpr(ABitAndNotExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" &^ ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseAAddExpr(AAddExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" + ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseASubExpr(ASubExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" - ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseABitOrExpr(ABitOrExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" | ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseABitXorExpr(ABitXorExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" ^ ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseAEqExpr(AEqExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" == ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseANeqExpr(ANeqExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" != ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseALessExpr(ALessExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" < ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseALessEqExpr(ALessEqExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" <= ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseAGreatExpr(AGreatExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" > ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseAGreatEqExpr(AGreatEqExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" >= ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseALogicAndExpr(ALogicAndExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" && ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseALogicOrExpr(ALogicOrExpr node) {
        printer.print("(");
        node.getLeft().apply(this);
        printer.print(" || ");
        node.getRight().apply(this);
        printer.print(")");
    }

    @Override
    public void caseAIntType(AIntType node) {
        printer.print(node.getInt().getText());
    }

    @Override
    public void caseAFloat64Type(AFloat64Type node) {
        printer.print(node.getFloat64().getText());
    }

    @Override
    public void caseABoolType(ABoolType node) {
        printer.print(node.getBool().getText());
    }

    @Override
    public void caseARuneType(ARuneType node) {
        printer.print(node.getRune().getText());
    }

    @Override
    public void caseAStringType(AStringType node) {
        printer.print(node.getString().getText());
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
}
