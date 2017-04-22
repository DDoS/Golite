package golite.semantic.check;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import golite.analysis.AnalysisAdapter;
import golite.analysis.DepthFirstAdapter;
import golite.node.ABlockStmt;
import golite.node.ABreakStmt;
import golite.node.AClauseForCondition;
import golite.node.ADefaultCase;
import golite.node.AEmptyForCondition;
import golite.node.AEnclosedExpr;
import golite.node.AExprCase;
import golite.node.AForStmt;
import golite.node.AFuncDecl;
import golite.node.AIfBlock;
import golite.node.AIfStmt;
import golite.node.AReturnStmt;
import golite.node.ASwitchStmt;
import golite.node.Node;
import golite.node.PCase;
import golite.node.PForCondition;
import golite.node.PIfBlock;
import golite.node.PStmt;

/**
 * Checks that a function will return a value on all path.
 */
public class TerminatingStmtChecker extends AnalysisAdapter {
    private final Set<Node> terminating = new HashSet<>();

    @Override
    public void caseAFuncDecl(AFuncDecl node) {
        if (!endsInTerminating(node.getStmt())) {
            throw new TypeCheckerException(node, "Missing return statement");
        }
    }

    @Override
    public void caseAReturnStmt(AReturnStmt node) {
        // Always is terminating
        terminating.add(node);
    }

    @Override
    public void caseABlockStmt(ABlockStmt node) {
        // The block must end with a terminating stmt
        if (endsInTerminating(node.getStmt())) {
            terminating.add(node);
        }
    }

    @Override
    public void caseAIfStmt(AIfStmt node) {
        // The else must be terminating
        if (!endsInTerminating(node.getElse())) {
            return;
        }
        // So must every if-block
        for (PIfBlock ifBlock : node.getIfBlock()) {
            if (!endsInTerminating(((AIfBlock) ifBlock).getBlock())) {
                return;
            }
        }
        terminating.add(node);
    }

    @Override
    public void caseAForStmt(AForStmt node) {
        // The condition must be empty
        final PForCondition condition = node.getForCondition();
        final boolean emptyCondition = condition instanceof AEmptyForCondition
                || condition instanceof AClauseForCondition && ((AClauseForCondition) condition).getCond() == null;
        if (!emptyCondition) {
            return;
        }
        // The loop cannot break
        if (BreakFinder.breaks(node.getStmt())) {
            return;
        }
        terminating.add(node);
    }

    @Override
    public void caseASwitchStmt(ASwitchStmt node) {
        // It must have a default case
        if (node.getCase().stream().noneMatch(case_ -> case_ instanceof ADefaultCase)) {
            return;
        }
        // All cases must not break and end in a terminating stmt
        for (PCase case_ : node.getCase()) {
            final List<PStmt> stmts = case_ instanceof AExprCase ? ((AExprCase) case_).getStmt() : ((ADefaultCase) case_).getStmt();
            if (BreakFinder.breaks(stmts) || !endsInTerminating(stmts)) {
                return;
            }
        }
        terminating.add(node);
    }

    @Override
    public void caseAEnclosedExpr(AEnclosedExpr node) {
        throw new IllegalStateException("Enclosed expressions should have been removed earlier");
    }

    @Override
    public void caseADefaultCase(ADefaultCase node) {
        // Anything else isn't a terminating stmt
    }

    private boolean endsInTerminating(List<PStmt> stmts) {
        // The statement list cannot be empty
        if (stmts.isEmpty()) {
            return false;
        }
        // The last one must terminate
        final PStmt lastStmt = stmts.get(stmts.size() - 1);
        lastStmt.apply(this);
        return terminating.contains(lastStmt);
    }

    private static class BreakFinder extends DepthFirstAdapter {
        private boolean breaks = false;

        @Override
        public void outABreakStmt(ABreakStmt node) {
            breaks = true;
        }

        @Override
        public void caseAForStmt(AForStmt node) {
            // Don't enter for-loops, since the break only affects the inner most switch or for-loop
        }

        @Override
        public void caseASwitchStmt(ASwitchStmt node) {
            // Don't enter switches, since the break only affects the inner most switch or for-loop
        }

        @Override
        public void caseAEnclosedExpr(AEnclosedExpr node) {
            throw new IllegalStateException("Enclosed expressions should have been removed earlier");
        }

        private static boolean breaks(List<PStmt> stmts) {
            final BreakFinder finder = new BreakFinder();
            for (PStmt stmt : stmts) {
                stmt.apply(finder);
                if (finder.breaks) {
                    return true;
                }
            }
            return false;
        }
    }
}
