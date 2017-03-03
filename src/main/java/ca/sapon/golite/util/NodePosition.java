package ca.sapon.golite.util;

import golite.analysis.DepthFirstAdapter;
import golite.node.Node;
import golite.node.Token;

/**
 * Traverse the AST to extract source position information for the node.
 */
public class NodePosition extends DepthFirstAdapter implements SourcePositioned {
    private int minLine = Integer.MAX_VALUE;
    private int maxLine = Integer.MIN_VALUE;
    private int minPos = Integer.MAX_VALUE;
    private int maxPos = Integer.MIN_VALUE;

    public NodePosition(Node node) {
        node.apply(this);
    }

    @Override
    public int getStartLine() {
        return minLine;
    }

    @Override
    public int getEndLine() {
        return maxLine;
    }

    @Override
    public int getStartPos() {
        return minPos;
    }

    @Override
    public int getEndPos() {
        return maxPos;
    }

    @Override
    public void defaultCase(Node node) {
        if (!(node instanceof Token)) {
            return;
        }
        final Token token = (Token) node;
        final int line = token.getLine();
        final int pos = token.getPos();
        minLine = Math.min(minLine, line);
        maxLine = Math.max(maxLine, line);
        minPos = Math.min(minPos, pos);
        maxPos = Math.max(maxPos, pos);
    }
}
