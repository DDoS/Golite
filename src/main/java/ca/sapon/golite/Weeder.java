package ca.sapon.golite;

import java.util.ArrayDeque;
import java.util.Deque;

import golite.analysis.AnalysisAdapter;
import golite.analysis.DepthFirstAdapter;
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
import golite.node.ABreakStmt;
import golite.node.AContinueStmt;
import golite.node.ADeclVarShortStmt;
import golite.node.AExprStmt;
import golite.node.AForStmt;
import golite.node.AFuncDecl;
import golite.node.AIdentExpr;
import golite.node.AIfStmt;
import golite.node.AIndexExpr;
import golite.node.AProg;
import golite.node.AReturnStmt;
import golite.node.ASelectExpr;
import golite.node.ASwitchStmt;
import golite.node.Node;

/**
 * TODO: weeding of
 * 1. Assignment LHS
 * 2. Short variable declaration LHS
 * 3. Expression statements
 * 4. Default case
 * 5. Break and continue
 */
public class Weeder extends DepthFirstAdapter {
    @Override
    public void outAAssignStmt(AAssignStmt node) {
        node.getLeft().forEach(n -> n.apply(AssignableWeeder.INSTANCE));
    }

    @Override
    public void outAAssignMulStmt(AAssignMulStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignDivStmt(AAssignDivStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignRemStmt(AAssignRemStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignLshiftStmt(AAssignLshiftStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignRshiftStmt(AAssignRshiftStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignBitAndStmt(AAssignBitAndStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignBitAndNotStmt(AAssignBitAndNotStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignAddStmt(AAssignAddStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignSubStmt(AAssignSubStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignBitOrStmt(AAssignBitOrStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outAAssignBitXorStmt(AAssignBitXorStmt node) {
        node.getLeft().apply(AssignableWeeder.INSTANCE);
    }
    private static class AssignableWeeder extends AnalysisAdapter {
        private static final AssignableWeeder INSTANCE = new AssignableWeeder();

        @Override
        public void caseAIdentExpr(AIdentExpr node) {
            // Always valid, don't throw
        }

        @Override
        public void caseASelectExpr(ASelectExpr node) {
            // Valid if the value expression is also assignable
            node.getValue().apply(this);
        }

        @Override
        public void caseAIndexExpr(AIndexExpr node) {
            // Valid if the value expression is also assignable
            node.getValue().apply(this);
        }

        @Override
        public void defaultCase(Node node) {
            throw new WeederException("Not an assignable expression");
        }
    }
}
