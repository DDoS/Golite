package ca.sapon.golite.semantic.check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import golite.analysis.AnalysisAdapter;
import golite.node.ABlockStmt;
import golite.node.ABreakStmt;
import golite.node.AContinueStmt;
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
        // Remove path nodes between breaks and continues and the end of the for or switch block
        root = shortenBreakAndContinue(root);

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
        // Mark all the final nodes as ending the switch-block
        ends.forEach(end -> end.markEnd(BlockEnd.SWITCH));
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
        // Traverse the for-block
        final List<Path> ends = new ArrayList<>();
        traverseConditionalBlock(node.getStmt(), ends);
        // Mark all the final nodes as ending the for-block
        ends.forEach(end -> end.markEnd(BlockEnd.FOR));
        // Add the exiting paths to the current ones
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
        end.ends.retainAll(breakEnds);
        // The shortened path now ends the block, so copy over the ends from the long path
        final Set<BlockEnd> newEnds = EnumSet.copyOf(end.ends);
        path.ends.clear();
        path.ends.addAll(newEnds);
        // We are done shortening the path
        return path;
    }

    private static Path searchForAnyEnd(Path path, Set<BlockEnd> ends) {
        // If the path contains any of the block ends, then we found the end path node
        for (BlockEnd end : ends) {
            if (path.ends.contains(end)) {
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

    private static class Path {
        private final PStmt stmt;
        private final List<Path> children = new ArrayList<>();
        private final Set<BlockEnd> ends = EnumSet.noneOf(BlockEnd.class);

        private Path(PStmt stmt) {
            this.stmt = stmt;
        }

        private Path addChild(PStmt child) {
            final Path next = new Path(child);
            children.add(next);
            return next;
        }

        private void markEnd(BlockEnd end) {
            ends.add(end);
        }

        @Override
        public String toString() {
            return toString(0);
        }

        private String toString(int depth) {
            String s = stmt.getClass().getSimpleName() + "(" + stmt + ")";
            if (!ends.isEmpty()) {
                s += " end ";
                s += String.join(", ", ends.stream().map(Object::toString).collect(Collectors.toSet()));
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
