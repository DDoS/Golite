package golite.ir;

import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import golite.ir.node.FunctionDecl;
import golite.ir.node.Jump;
import golite.ir.node.JumpCond;
import golite.ir.node.Label;
import golite.ir.node.Stmt;
import golite.ir.node.ValueReturn;
import golite.ir.node.VoidReturn;
import golite.util.SourcePrinter;

/**
 *
 */
public class IrFlowSanitizer {
    public static void sanitize(FunctionDecl function) {
        // Start by splitting the function into blocks, which are separated by labels
        final List<Stmt> stmts = function.getStatements();
        final List<Block> blocks = createBlocks(stmts);
        // The first step is to replace the implicit fallthrough's by jumps to the next block
        makeFallthroughExplicit(blocks);
        // Then remove all unreachable statements in the blocks (the ones after terminators)
        blocks.forEach(Block::removeUnreachable);
        // This will remove references to some blocks, which are now also unreachable, so we remove those too
        removeUnreachableBlocks(blocks);
        // Finally we can re-collapse the blocks in to the stmt list
        stmts.clear();
        blocks.forEach(block -> block.addToStmts(stmts));

        //blocks.forEach(block -> System.out.println(block.toString()));
    }

    private static List<Block> createBlocks(List<Stmt> stmts) {
        final List<Block> blocks = new ArrayList<>();
        Label currentBlockLabel = null;
        final List<Stmt> currentBlockStmts = new ArrayList<>();
        for (Stmt stmt : stmts) {
            if (stmt instanceof Label) {
                blocks.add(new Block(currentBlockLabel, currentBlockStmts));
                currentBlockStmts.clear();
                currentBlockLabel = (Label) stmt;
            } else {
                currentBlockStmts.add(stmt);
            }
        }
        blocks.add(new Block(currentBlockLabel, currentBlockStmts));
        return blocks;
    }

    private static void makeFallthroughExplicit(List<Block> blocks) {
        for (int i = 0; i < blocks.size() - 1; i++) {
            final Block block = blocks.get(i);
            if (block.hasTerminator()) {
                continue;
            }
            block.endWithJumpTo(blocks.get(i + 1).label);
        }
    }

    private static void removeUnreachableBlocks(List<Block> blocks) {
        // Create a queue of reachable blocks to visit next (to reach for more blocks)
        final Queue<Block> flow = new ArrayDeque<>();
        flow.add(blocks.get(0));
        // Since we can have cycles, we need to keep track of the blocks we already reached
        final Set<Block> reached = new HashSet<>();
        // This map is just to speed-up looks ups from label to its block
        final Map<Label, Block> labelToBlock = blocks.stream()
                .collect(Collectors.toMap(block -> block.label, Function.identity()));
        // Now find all the reachable blocks
        while (!flow.isEmpty()) {
            // Reach the block
            final Block block = flow.poll();
            reached.add(block);
            // Then get all the labels it points to
            final Set<Label> jumpLabels = block.getJumpLabels();
            // For each label being used, get the corresponding block, and add it to the flow if not already reached
            jumpLabels.stream()
                    .map(labelToBlock::get)
                    .filter(nextBlock -> !reached.contains(nextBlock))
                    .forEach(flow::add);
        }
        // Finally discard all blocks that were not reached
        blocks.removeIf(block -> !reached.contains(block));
    }

    private static boolean isBlockTerminator(Stmt stmt) {
        return stmt instanceof Jump || stmt instanceof VoidReturn || stmt instanceof ValueReturn;
    }

    private static class Block {
        private final Label label;
        private final List<Stmt> stmts;

        private Block(Label label, List<Stmt> stmts) {
            this.label = label;
            this.stmts = new ArrayList<>(stmts);
        }

        private void removeUnreachable() {
            int lastReachableIndex = -1;
            for (int i = 0; i < stmts.size(); i++) {
                lastReachableIndex = i;
                if (isBlockTerminator(stmts.get(i))) {
                    break;
                }
            }
            stmts.subList(lastReachableIndex + 1, stmts.size()).clear();
        }

        private boolean hasTerminator() {
            return !stmts.isEmpty() && isBlockTerminator(stmts.get(stmts.size() - 1));
        }

        private void endWithJumpTo(Label label) {
            stmts.add(new Jump(label));
        }

        private Set<Label> getJumpLabels() {
            final Set<Label> labels = new HashSet<>();
            for (Stmt stmt : stmts) {
                if (stmt instanceof Jump) {
                    labels.add(((Jump) stmt).getLabel());
                } else if (stmt instanceof JumpCond) {
                    labels.add(((JumpCond) stmt).getLabel());
                }
            }
            return labels;
        }

        private void addToStmts(List<Stmt> stmts) {
            if (label != null) {
                stmts.add(label);
            }
            stmts.addAll(this.stmts);
        }

        @Override
        public String toString() {
            final StringWriter writer = new StringWriter();
            final SourcePrinter printer = new SourcePrinter(writer);
            if (label == null) {
                printer.print(">");
            } else {
                printer.print(label.getName());
            }
            printer.print(" {").newLine().indent();
            stmts.forEach(stmt -> {
                stmt.print(printer);
                printer.newLine();
            });
            printer.dedent().print("}").newLine();
            return writer.toString();
        }
    }
}
