package ca.sapon.golite.syntax;

import ca.sapon.golite.util.NodePosition;
import golite.node.Node;

/**
 * An exception thrown by the weeder.
 */
public class WeederException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public WeederException(Node node, String message) {
        super(buildDetailedMessage(node, message));
    }

    private static String buildDetailedMessage(Node node, String message) {
        final NodePosition position = new NodePosition();
        node.apply(position);
        final StringBuilder detailed = new StringBuilder()
                .append('[').append(position.getMinLine()).append(',').append(position.getMinPos()).append(']');
        if (position.getMinLine() != position.getMaxLine() || position.getMinPos() != position.getMaxPos()) {
            detailed.append(" to ").append('[').append(position.getMaxLine()).append(',').append(position.getMaxPos()).append(']');
        }
        return detailed.append(": ").append(message).toString();
    }
}
