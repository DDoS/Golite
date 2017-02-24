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
import golite.node.ACallExpr;
import golite.node.AContinueStmt;
import golite.node.ADeclVarShortStmt;
import golite.node.ADecrStmt;
import golite.node.ADefaultCase;
import golite.node.AExprStmt;
import golite.node.AForStmt;
import golite.node.AFuncDecl;
import golite.node.AIdentExpr;
import golite.node.AIfStmt;
import golite.node.AIncrStmt;
import golite.node.AIndexExpr;
import golite.node.AProg;
import golite.node.AReturnStmt;
import golite.node.ASelectExpr;
import golite.node.ASwitchStmt;
import golite.node.Node;

public class Weeder extends DepthFirstAdapter {
    private final Deque<Scope> scopeStack = new ArrayDeque<>();

    @Override
    public void inAProg(AProg node) {
        scopeStack.push(Scope.TOP);
    }

    @Override
    public void outAProg(AProg node) {
        popScope(Scope.TOP);
    }

    @Override
    public void inAFuncDecl(AFuncDecl node) {
        scopeStack.push(Scope.FUNC);
    }

    @Override
    public void outAFuncDecl(AFuncDecl node) {
        popScope(Scope.FUNC);
    }

    @Override
    public void inAIfStmt(AIfStmt node) {
        scopeStack.push(Scope.IF);
    }

    @Override
    public void outAIfStmt(AIfStmt node) {
        popScope(Scope.IF);
    }

    @Override
    public void inAForStmt(AForStmt node) {
        scopeStack.push(Scope.FOR);
    }

    @Override
    public void outAForStmt(AForStmt node) {
        popScope(Scope.FOR);
    }

    @Override
    public void inASwitchStmt(ASwitchStmt node) {
        scopeStack.push(Scope.SWITCH);
        boolean alreadyDefault = false;
        for (Node case_ : node.getCase()) {
            if (case_.getClass() == ADefaultCase.class) {
                if (alreadyDefault) {
                    throw new WeederException(case_, "There can only be one default case in a switch statement");
                }
                alreadyDefault = true;
            }
        }
    }

    @Override
    public void outASwitchStmt(ASwitchStmt node) {
        popScope(Scope.SWITCH);
    }

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

    @Override
    public void outAIncrStmt(AIncrStmt node) {
        node.getExpr().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outADecrStmt(ADecrStmt node) {
        node.getExpr().apply(AssignableWeeder.INSTANCE);
    }

    @Override
    public void outADeclVarShortStmt(ADeclVarShortStmt node) {
        for (Node n : node.getLeft()) {
            if (n.getClass() != AIdentExpr.class) {
                throw new WeederException(n, "The left side of the declaration must contain identifiers");
            }
        }
    }

    @Override
    public void outAExprStmt(AExprStmt node) {
        if (node.getExpr().getClass() != ACallExpr.class) {
            throw new WeederException(node.getExpr(), "Expected an expression");
        }
    }

    @Override
    public void outABreakStmt(ABreakStmt node) {
        if (!scopeStack.contains(Scope.FOR)) {
            throw new WeederException(node, "The break keyword cannot be used outside a loop");
        }
    }

    @Override
    public void outAContinueStmt(AContinueStmt node) {
        if (!scopeStack.contains(Scope.FOR)) {
            throw new WeederException(node, "The continue keyword cannot be used outside a loop");
        }
    }

    @Override
    public void outAReturnStmt(AReturnStmt node) {
        if (!scopeStack.contains(Scope.FUNC)) {
            throw new WeederException(node, "The return keyword cannot be used outside a function");
        }
    }

    private void popScope(Scope out) {
        final Scope scope = scopeStack.pop();
        if (scope != out) {
            throw new IllegalStateException("Expected out of scope " + out + ", but got " + scope);
        }
    }

    private enum Scope {
        TOP, FUNC, IF, FOR, SWITCH
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
            node.getValue().apply(INSTANCE);
        }

        @Override
        public void caseAIndexExpr(AIndexExpr node) {
            // Valid if the value expression is also assignable
            node.getValue().apply(INSTANCE);
        }

        @Override
        public void defaultCase(Node node) {
            throw new WeederException(node, "Not an assignable expression");
        }
    }
}
