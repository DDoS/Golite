package ca.sapon.golite.syntax.weed;

import ca.sapon.golite.util.NodePosition;
import ca.sapon.golite.util.SourcePositioned;
import golite.node.Node;

/**
 * An exception thrown by the weeder.
 */
public class WeederException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public WeederException(Node node, String message) {
        super(SourcePositioned.appendPosition(new NodePosition(node), message));
    }
}
