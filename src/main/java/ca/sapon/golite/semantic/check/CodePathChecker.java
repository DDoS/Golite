package ca.sapon.golite.semantic.check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import golite.analysis.AnalysisAdapter;
import golite.node.ABlockStmt;
import golite.node.ABreakStmt;
import golite.node.AClauseForCondition;
import golite.node.AContinueStmt;
import golite.node.ADefaultCase;
import golite.node.AEmptyForCondition;
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
 *
 */
public class CodePathChecker extends AnalysisAdapter {
    private final boolean mustReturn;
    private final List<Path> currents = new ArrayList<>();
    private final Set<PStmt> statements = new LinkedHashSet<>();

    public CodePathChecker(boolean mustReturn) {
        this.mustReturn = mustReturn;
    }

    @Override
    public void caseAFuncDecl(AFuncDecl node) {
        final List<PStmt> stmts = node.getStmt();
        // Trivial case: an empty function
        if (stmts.isEmpty()) {
            if (mustReturn) {
                throw new TypeCheckerException(node, "Cannot have an empty function body if it returns a value");
            }
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
        // Mark all the last path nodes as ending the function
        currents.forEach(Path::endsFunc);
        // Remove path nodes between breaks and continues and the end of the for or switch block
        root = shortenBreakAndContinue(root);
        // Check that all paths return
        final boolean allReturn = tracePathsToReturn(root);
        if (mustReturn && !allReturn) {
            throw new TypeCheckerException(node, "Missing return statement on one or more paths");
        }
        // Check that all statements where reached when traversing the return paths
        if (!statements.isEmpty()) {
            throw new TypeCheckerException(statements.iterator().next(), "Unreachable statement");
        }
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
        // Mark all the final nodes as ending the switch-block
        ends.forEach(end -> end.endsBlock(BlockEnd.SWITCH));
        // Set the paths to the exiting ones, which we saved earlier for each block
        currents.clear();
        currents.addAll(ends);
    }

    @Override
    public void caseAForStmt(AForStmt node) {
        // No new paths to create if empty
        if (node.getStmt().isEmpty()) {
            return;
        }
        // Check if the for-block is entered conditionally
        final PForCondition condition = node.getForCondition();
        // If the condition is missing, then it is always true
        final boolean unconditional = condition instanceof AEmptyForCondition
                || condition instanceof AClauseForCondition && ((AClauseForCondition) condition).getCond() == null;
        // Traverse the for-block
        if (unconditional) {
            traverseBlock(node.getStmt());
            // Mark all the current nodes as ending the for-block
            currents.forEach(current -> current.endsBlock(BlockEnd.FOR));
        } else {
            final List<Path> ends = new ArrayList<>();
            traverseConditionalBlock(node.getStmt(), ends);
            // Mark all the final nodes as ending the for-block
            ends.forEach(end -> end.endsBlock(BlockEnd.FOR));
            // Add the exiting paths to the current ones
            currents.addAll(ends);
        }
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
            // Add the stmt to the set of all stmts in the function
            if (!statements.add(stmt)) {
                throw new IllegalStateException("Statements are supposed to be unique");
            }
            // Apply to the stmt to traverse nested blocks
            stmt.apply(this);
        }
    }

    private static Path shortenBreakAndContinue(Path path) {
        // Start by shortening the child paths
        final List<Path> children = path.children;
        for (int i = 0; i < children.size(); i++) {
            children.set(i, shortenBreakAndContinue(children.get(i)));
        }
        // Based on the stmt, find which block kind we are breaking or continuing (aborting) from
        final EnumSet<BlockEnd> breakEnds;
        if (path.stmt instanceof ABreakStmt) {
            breakEnds = EnumSet.of(BlockEnd.FOR, BlockEnd.SWITCH);
        } else if (path.stmt instanceof AContinueStmt) {
            breakEnds = EnumSet.of(BlockEnd.FOR);
        } else {
            // No path to shorten, just return as is
            return path;
        }
        // Find for the end of the block we are aborting (first one of the kind we encounter, on any path)
        final Path end = searchForAnyEnd(path, breakEnds);
        // If the bock end if the last path node, then so must the shortened path be
        if (end.children.isEmpty()) {
            path.children.clear();
        } else {
            // Otherwise its last child is always the one right after the block (the others might go deeper into nested blocks)
            final Path shortChild = end.children.get(end.children.size() - 1);
            // Replace the children of the abort stmt by the path after the block
            path.children.clear();
            path.children.add(shortChild);
        }
        // Remove ends that aren't relevant to the block
        end.blockEnds.retainAll(breakEnds);
        // The shortened path now ends the block, so copy over the ends from the long path
        final Set<BlockEnd> newEnds = EnumSet.copyOf(end.blockEnds);
        path.blockEnds.clear();
        path.blockEnds.addAll(newEnds);
        // We are done shortening the path
        return path;
    }

    private static Path searchForAnyEnd(Path path, Set<BlockEnd> ends) {
        // If the path contains any of the block ends, then we found the end path node
        for (BlockEnd end : ends) {
            if (path.blockEnds.contains(end)) {
                return path;
            }
        }
        // We should never reach the end: the block must terminate before the function end
        if (path.children.isEmpty()) {
            throw new IllegalStateException("Should have found the end for " + ends);
        }
        // Since the block terminates on all paths, we can just search on the first one
        return searchForAnyEnd(path.children.get(0), ends);
    }

    private boolean tracePathsToReturn(Path path) {
        // Remove the stmt from the set of all stmts to mark it as reachable
        statements.remove(path.stmt);
        // If the stmt is a return one, then the path ends here (possibly shortened)
        if (path.stmt instanceof AReturnStmt) {
            return true;
        }
        // If we reach the function end, then the path ends here, without reaching a return stmt
        boolean pathReturns = !path.funcEnd;
        // The children also all need to have a path to a return stmt
        for (Path child : path.children) {
            pathReturns &= tracePathsToReturn(child);
        }
        return pathReturns;
    }

    private static class Path {
        private final PStmt stmt;
        private final List<Path> children = new ArrayList<>();
        private final Set<BlockEnd> blockEnds = EnumSet.noneOf(BlockEnd.class);
        private boolean funcEnd = false;

        private Path(PStmt stmt) {
            this.stmt = stmt;
        }

        private Path addChild(PStmt child) {
            final Path next = new Path(child);
            children.add(next);
            return next;
        }

        private void endsBlock(BlockEnd end) {
            blockEnds.add(end);
        }

        private void endsFunc() {
            funcEnd = true;
        }

        @Override
        public String toString() {
            return toString(0);
        }

        private String toString(int depth) {
            String s = stmt.getClass().getSimpleName() + "(" + stmt + ")";
            final Set<String> endStrings = blockEnds.stream().map(Object::toString).collect(Collectors.toSet());
            if (funcEnd) {
                endStrings.add("FUNC");
            }
            if (!endStrings.isEmpty()) {
                s += " ends " + String.join(", ", endStrings);
            }
            s += System.lineSeparator();
            for (Path child : children) {
                for (int i = 0; i < depth + 1; i++) {
                    s += "    ";
                }
                s += child.toString(depth + 1);
            }
            return s;
        }
    }

    private enum BlockEnd {
        FOR, SWITCH
    }
}
