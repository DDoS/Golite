package ca.sapon.golite.semantic.check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import golite.analysis.AnalysisAdapter;
import golite.node.ABlockStmt;
import golite.node.ADefaultCase;
import golite.node.AExprCase;
import golite.node.AForStmt;
import golite.node.AFuncDecl;
import golite.node.AIfBlock;
import golite.node.AIfStmt;
import golite.node.ASwitchStmt;
import golite.node.Node;
import golite.node.PCase;
import golite.node.PIfBlock;
import golite.node.PStmt;

/**
 *
 */
public class CodePathChecker extends AnalysisAdapter {
    private List<Path> currents = new ArrayList<>();

    @Override
    public void caseAFuncDecl(AFuncDecl node) {
        final List<PStmt> stmts = node.getStmt();
        if (stmts.isEmpty()) {
            return;
        }
        // Create the root node
        final PStmt firstStmt = stmts.get(0);
        Path root = new Path(firstStmt);
        // Add it to the current paths
        currents.add(root);
        // Apply to the stmt to traverse nested blocks
        firstStmt.apply(this);
        // Traverse the rest of the block normally
        traverseBlock(stmts.subList(1, stmts.size()));

        currents.forEach(Path::terminate);
        System.out.println(root);
    }

    @Override
    public void caseABlockStmt(ABlockStmt node) {
        // Unconditionally traverse the block
        traverseBlock(node.getStmt());
    }

    @Override
    public void caseAIfStmt(AIfStmt node) {
        // Traverse each if-block
        final List<Path> ends = new ArrayList<>();
        for (PIfBlock ifBlock : node.getIfBlock()) {
            traverseConditionalBlock(((AIfBlock) ifBlock).getBlock(), ends);
        }
        // Traverse the else-block
        traverseConditionalBlock(node.getElse(), ends);
        // Set the paths to the exiting ones, which we saved earlier for each block
        currents.clear();
        currents.addAll(ends);
    }

    @Override
    public void caseASwitchStmt(ASwitchStmt node) {
        // Traverse each case block
        final List<Path> ends = new ArrayList<>();
        boolean hasDefault = false;
        for (PCase case_ : node.getCase()) {
            if (case_ instanceof ADefaultCase) {
                traverseConditionalBlock(((ADefaultCase) case_).getStmt(), ends);
                hasDefault = true;
            } else {
                traverseConditionalBlock(((AExprCase) case_).getStmt(), ends);
            }
        }
        // If we haven't traversed a default block, we'll do so for an implicit empty one
        if (!hasDefault) {
            traverseConditionalBlock(Collections.emptyList(), ends);
        }
        // Set the paths to the exiting ones, which we saved earlier for each block
        currents.clear();
        currents.addAll(ends);
    }

    private void traverseConditionalBlock(List<PStmt> block, List<Path> ends) {
        // Save the original path entering the block
        final List<Path> originals = new ArrayList<>(currents);
        // Traverse the block
        traverseBlock(block);
        // Save the resulting paths
        ends.addAll(currents);
        // Set the paths to the original ones entering the block
        currents.clear();
        currents.addAll(originals);
    }

    @Override
    public void caseAForStmt(AForStmt node) {
        // No new paths to create if empty
        if (node.getStmt().isEmpty()) {
            return;
        }
        // Traverse the for-block
        final List<Path> ends = new ArrayList<>();
        traverseConditionalBlock(node.getStmt(), ends);
        // Add the exiting paths to the current ones
        currents.addAll(ends);
    }

    @Override
    public void defaultCase(Node node) {
    }

    private void traverseBlock(List<PStmt> block) {
        for (PStmt stmt : block) {
            for (int i = 0; i < currents.size(); i++) {
                // Add the stmt as a child to the path node
                final Path next = currents.get(i).addChild(stmt);
                // Then swap the path node for its child
                currents.set(i, next);
            }
            // Apply to the stmt to traverse nested blocks
            stmt.apply(this);
        }
    }

    private static class Path {
        private final PStmt stmt;
        private final List<Path> children = new ArrayList<>();

        private Path(PStmt stmt) {
            this.stmt = stmt;
        }

        private Path addChild(PStmt child) {
            final Path next = new Path(child);
            children.add(next);
            return next;
        }

        private void terminate() {
            children.add(new Path(null));
        }

        @Override
        public String toString() {
            return toString(0);
        }

        private String toString(int depth) {
            if (stmt == null) {
                return "End" + System.lineSeparator();
            }
            String s = stmt.getClass().getSimpleName() + "(" + stmt + ")" + System.lineSeparator();
            for (Path child : children) {
                for (int i = 0; i < depth + 1; i++) {
                    s += "    ";
                }
                s += child.toString(depth + 1);
            }
            return s;
        }
    }
}
