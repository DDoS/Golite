package golite.util;

import golite.analysis.DepthFirstAdapter;
import golite.node.Node;
import golite.node.Token;

/**
 * Traverse the AST to extract source position information for the node.
 */
public class NodePosition extends DepthFirstAdapter implements SourcePositioned {
    private int startLine = Integer.MAX_VALUE;
    private int endLine = Integer.MIN_VALUE;
    private int startPos = Integer.MAX_VALUE;
    private int endPos = Integer.MIN_VALUE;

    public NodePosition(Node node) {
        node.apply(this);
    }

    @Override
    public int getStartLine() {
        return startLine;
    }

    @Override
    public int getEndLine() {
        return endLine;
    }

    @Override
    public int getStartPos() {
        return startPos;
    }

    @Override
    public int getEndPos() {
        return endPos;
    }

    @Override
    public void defaultCase(Node node) {
        if (!(node instanceof Token)) {
            return;
        }
        final Token token = (Token) node;
        final int line = token.getLine();
        final int pos = token.getPos();
        startLine = Math.min(startLine, line);
        endLine = Math.max(endLine, line);
        startPos = Math.min(startPos, pos);
        endPos = Math.max(endPos, pos);
    }
}
